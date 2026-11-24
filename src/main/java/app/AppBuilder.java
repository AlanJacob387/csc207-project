package app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.UncheckedIOException;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import data_access.AppDataStore;
import data_access.DatabaseConfig;
import data_access.FileAppDataAccessObject;
import data_access.SupabaseAppDataAccessObject;
import entity.Role;
import entity.Team;
import entity.UserFactory;
import interface_adapter.Session;
import interface_adapter.ViewManagerModel;
import interface_adapter.change_password.ChangePasswordController;
import interface_adapter.change_password.ChangePasswordPresenter;
import interface_adapter.change_password.ChangePasswordViewModel;
import interface_adapter.create_event.CreateEventController;
import interface_adapter.create_event.CreateEventPresenter;
import interface_adapter.create_event.CreateEventViewModel;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.logout.LogoutController;
import interface_adapter.logout.LogoutPresenter;
import interface_adapter.manage_users.ManageUsersController;
import interface_adapter.manage_users.ManageUsersPresenter;
import interface_adapter.manage_users.ManageUsersViewModel;
import interface_adapter.post_announcement.PostAnnouncementController;
import interface_adapter.post_announcement.PostAnnouncementPresenter;
import interface_adapter.post_announcement.PostAnnouncementViewModel;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import interface_adapter.view_announcements.ViewAnnouncementsController;
import interface_adapter.view_announcements.ViewAnnouncementsPresenter;
import interface_adapter.view_announcements.ViewAnnouncementsViewModel;
import use_case.change_password.ChangePasswordInteractor;
import use_case.create_event.CreateEventInteractor;
import use_case.login.LoginInteractor;
import use_case.logout.LogoutInteractor;
import use_case.manage_users.ChangeUserRoleInteractor;
import use_case.manage_users.CreateUserInteractor;
import use_case.manage_users.DeactivateUserInteractor;
import use_case.manage_users.ListAccountsInteractor;
import use_case.manage_users.ReactivateUserInteractor;
import use_case.post_announcement.PostAnnouncementInteractor;
import use_case.signup.SignupInteractor;
import use_case.view_announcements.ViewAnnouncementsInteractor;
import view.AnnouncementsView;
import view.AppShell;
import view.ChangePasswordView;
import view.CalendarView;
import view.CreateEventView;
import view.DatabaseSettingsView;
import view.LoginView;
import view.ManageUsersView;
import view.SignupView;
import view.ViewManager;

/**
 * Builds the whole application: one add...View() method per page and one
 * add...UseCase() method per use case, so Main just states what the app has.
 *
 * Adding a feature means adding a pair of methods here instead of editing a
 * long main method. This mirrors the AppBuilder in the course''s user-login
 * example.
 *
 * Typical order: views, then the shell, then the use cases that fill them in,
 * then build().
 */
public class AppBuilder {

    /** Password given to the demo accounts seeded on first run. */
    private static final String DEMO_PASSWORD = "pw";

    /** Id of the demo team seeded on first run. */
    private static final String DEMO_TEAM_ID = "demo-team";

    // Shared collaborators, created once and handed to whatever needs them.
    private final DatabaseConfig databaseConfig = resolveDatabaseConfig();
    private final AppDataStore dataAccess = createStore(databaseConfig);
    private final ViewManagerModel viewManagerModel = new ViewManagerModel();
    private final Session session = new Session();
    private final UserFactory userFactory = new UserFactory();
    private final ViewManager viewManager = new ViewManager(viewManagerModel);

    private LoginView loginView;
    private LoginViewModel loginViewModel;
    private SignupView signupView;
    private SignupViewModel signupViewModel;
    private AppShell appShell;
    private CreateEventViewModel createEventViewModel;
    private ChangePasswordView changePasswordView;
    private ChangePasswordViewModel changePasswordViewModel;

    /** What went wrong before any page could open, such as an unreachable store. */
    private String startupNotice = "";

    /** Sets up the collaborators and makes sure a fresh install is usable. */
    public AppBuilder() {
        startupNotice = seedDemoDataIfEmpty(dataAccess, userFactory);
    }

    /**
     * Adds the login page.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addLoginView() {
        loginViewModel = new LoginViewModel();
        loginView = new LoginView(loginViewModel);
        loginView.setStoreDescription(dataAccess.describe());
        if (!startupNotice.isEmpty()) {
            loginView.setStartupProblem(startupNotice);
        }
        loginView.setOnOpenDatabaseSettings(() -> navigate(ViewManagerModel.DATABASE));
        viewManager.register(ViewManager.LOGIN, loginView);
        return this;
    }

    /**
     * Adds the page where a user connects the app to the shared database.
     *
     * Saving writes the settings into the user's home folder, so the next start
     * connects without a terminal. The store is built once at startup, so the page
     * tells the user to restart after saving.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addDatabaseSettingsView() {
        final DatabaseSettingsView view = new DatabaseSettingsView(
                databaseConfig == null ? "" : databaseConfig.getUrl(),
                DatabaseConfig.userFile().toString());
        view.setOnBackToLogin(() -> navigate(ViewManagerModel.LOGIN));
        view.setOnTestConnection((url, key) -> {
            final SupabaseAppDataAccessObject.ConnectionCheck check =
                    SupabaseAppDataAccessObject.testConnection(url, key);
            return new DatabaseSettingsView.TestResult(check.isOk(), check.getMessage());
        });
        view.setOnSave(DatabaseConfig::save);
        viewManager.register(ViewManager.DATABASE, view);
        return this;
    }

    /**
     * Adds the sign-up page.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addSignupView() {
        signupViewModel = new SignupViewModel();
        signupView = new SignupView(signupViewModel);
        viewManager.register(ViewManager.SIGNUP, signupView);
        return this;
    }

    /**
     * Adds the signed-in shell and the pages in its sidebar. Pages are listed
     * with the roles allowed to see them; no roles means everyone.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addAppShell() {
        appShell = new AppShell();
        session.addPropertyChangeListener(evt -> appShell.applyRole(session.getCurrentRole()));

        appShell.addPage("Calendar", new CalendarView());
        appShell.addPage("Events", placeholder("Events - TODO (event list page)"));
        appShell.addPage("My Stats",
                placeholder("My Stats - TODO (view_stats use case)"), Role.PLAYER);
        appShell.addPage("Roster",
                placeholder("Roster - TODO (manage_teams use case)"),
                Role.ADMIN, Role.COACH);

        viewManager.register(ViewManager.APP, appShell);
        return this;
    }

    /**
     * Wires the Log In use case into the login page.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addLoginUseCase() {
        final LoginPresenter presenter =
                new LoginPresenter(viewManagerModel, loginViewModel);
        final LoginController controller =
                new LoginController(new LoginInteractor(dataAccess, presenter, session));

        loginView.setLoginController(controller);
        loginView.setOnGoToSignup(() -> navigate(ViewManagerModel.SIGNUP));
        return this;
    }

    /**
     * Wires the Sign Up use case into the sign-up page.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addSignupUseCase() {
        final SignupPresenter presenter =
                new SignupPresenter(viewManagerModel, signupViewModel, loginViewModel);
        final SignupController controller =
                new SignupController(new SignupInteractor(dataAccess, presenter, userFactory));

        signupView.setSignupController(controller);
        signupView.setOnBackToLogin(() -> navigate(ViewManagerModel.LOGIN));
        return this;
    }

    /**
     * Wires the Create Event use case and adds its page to the shell.
     *
     * The Create Event view takes its controller in the constructor, so this
     * method both builds the page and connects the use case. Call it after
     * addAppShell().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addCreateEventUseCase() {
        createEventViewModel = new CreateEventViewModel();
        final CreateEventPresenter presenter = new CreateEventPresenter(createEventViewModel);
        final CreateEventController controller = new CreateEventController(
                new CreateEventInteractor(dataAccess, presenter));

        appShell.addPage("New Event",
                pad(new CreateEventView(controller, createEventViewModel)), Role.COACH);
        return this;
    }

    /** Adds the team announcement list and the authorized coach's posting form. */
    public AppBuilder addAnnouncementUseCases() {
        final PostAnnouncementViewModel postViewModel = new PostAnnouncementViewModel();
        final ViewAnnouncementsViewModel announcementsViewModel = new ViewAnnouncementsViewModel();
        final PostAnnouncementController postController = new PostAnnouncementController(
                new PostAnnouncementInteractor(dataAccess, new PostAnnouncementPresenter(postViewModel)), session);
        final ViewAnnouncementsController announcementsController = new ViewAnnouncementsController(
                new ViewAnnouncementsInteractor(dataAccess,
                        new ViewAnnouncementsPresenter(announcementsViewModel)), session);

        appShell.addPage("Announcements", new AnnouncementsView(postController, postViewModel,
                announcementsController, announcementsViewModel, session));
        return this;
    }

    /**
     * Adds the administrator page and wires the read and the four
     * account-management use cases. Call it after addAppShell(): the page is only
     * visible to administrators.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addManageUsersUseCase() {
        final ManageUsersViewModel viewModel = new ManageUsersViewModel();
        final ManageUsersPresenter presenter = new ManageUsersPresenter(viewModel);
        final ManageUsersController controller = new ManageUsersController(
                new CreateUserInteractor(dataAccess, presenter, session, userFactory),
                new ChangeUserRoleInteractor(dataAccess, presenter, session),
                new DeactivateUserInteractor(dataAccess, presenter, session),
                new ReactivateUserInteractor(dataAccess, presenter, session),
                new ListAccountsInteractor(dataAccess, presenter, session));

        appShell.addPage("Manage Users", new ManageUsersView(controller, viewModel, session),
                Role.ADMIN);
        return this;
    }

    /**
     * Wires the Log Out use case into the shell Log out button.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addLogoutUseCase() {
        final LogoutPresenter presenter =
                new LogoutPresenter(viewManagerModel, loginViewModel);
        final LogoutController controller =
                new LogoutController(new LogoutInteractor(session, presenter));

        appShell.setLogoutController(controller);
        return this;
    }

    /**
     * Adds the Account page, where a signed-in user changes their own password.
     * Call it after addAppShell().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addChangePasswordView() {
        changePasswordViewModel = new ChangePasswordViewModel();
        changePasswordView = new ChangePasswordView(changePasswordViewModel, session);
        appShell.addPage("Account", pad(changePasswordView));
        return this;
    }

    /**
     * Wires the Change Password use case into the account page.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addChangePasswordUseCase() {
        final ChangePasswordPresenter presenter =
                new ChangePasswordPresenter(changePasswordViewModel);
        final ChangePasswordController controller = new ChangePasswordController(
                new ChangePasswordInteractor(dataAccess, presenter, session));

        changePasswordView.setChangePasswordController(controller);
        return this;
    }

    public JFrame build() {
        final JFrame frame = new JFrame("Team Sports Management Platform");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(viewManager);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setLocationRelativeTo(null);

        navigate(ViewManagerModel.LOGIN);
        return frame;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Asks the view manager to show a page. Presenters navigate the same way. */
    private void navigate(String page) {
        viewManagerModel.setState(page);
        viewManagerModel.firePropertyChanged();
    }

    /**
     * Creates one demo account per role (password "pw") and a demo team, but
     * only where they are missing, so the app can be tried straight away.
     *
     * Reports a failure instead of throwing: a store that cannot be reached while
     * the app is starting must still leave the user on the sign-in page, where the
     * reason is shown and the database settings can be corrected. Nothing is
     * swapped for a local file - the caller keeps the store it chose.
     *
     * Every account is checked separately: a store that already holds a
     * customised player1 keeps it, instead of being overwritten because only
     * coach1 had been deleted. The factory hashes each demo password, so a fresh
     * store never holds plaintext credentials. Replace this with the team's real
     * seed data (task 4.4).
     *
     * @param dataAccess  the store the whole app will use
     * @param userFactory builds and hashes the demo accounts
     * @return an empty string on success, or what the sign-in page should say
     */
    static String seedDemoDataIfEmpty(AppDataStore dataAccess, UserFactory userFactory) {
        try {
            seedUser(dataAccess, userFactory, "coach1", "Coach One", Role.COACH);
            seedUser(dataAccess, userFactory, "player1", "Pat Player", Role.PLAYER);
            seedUser(dataAccess, userFactory, "parent1", "Sam Parent", Role.PARENT);
            seedUser(dataAccess, userFactory, "admin1", "Ada Admin", Role.ADMIN);

            if (dataAccess.getTeam(DEMO_TEAM_ID) == null) {
                final Team team = new Team(DEMO_TEAM_ID, "Demo Team", "Basketball", "coach1");
                team.addPlayer("player1");
                dataAccess.saveTeam(team);
            }
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            return "Could not reach the account store: " + ex.getMessage()
                    + " Sign-in will fail until it is back.";
        }
        return "";
    }

    /**
     * Chooses the store: the hosted database when settings are configured, the
     * local JSON file otherwise.
     *
     * The connection is chosen once, when the app starts, from the user's saved
     * settings, the environment or the system properties (see DatabaseConfig).
     * With nothing configured the local file is used, so a clean clone runs with
     * no account and the unit tests stay offline.
     *
     * There is deliberately no fallback at request time: if the database is
     * configured but unreachable, the failure is reported instead of quietly
     * switching to a local file, because two people who think they share data
     * would then silently diverge.
     *
     * @param config the resolved settings, or null when nothing is configured
     * @return the store the whole app should use
     */
    private static AppDataStore createStore(DatabaseConfig config) {
        return config == null
                ? new FileAppDataAccessObject()
                : new SupabaseAppDataAccessObject(config.getUrl(), config.getKey());
    }

    /**
     * Reads the database settings without letting a broken file stop startup: the
     * sign-in page then shows the local store and still offers the settings page
     * where the mistake can be corrected.
     *
     * @return the settings, or null when nothing usable is configured
     */
    private static DatabaseConfig resolveDatabaseConfig() {
        try {
            return DatabaseConfig.resolve();
        }
        catch (RuntimeException ex) {
            System.err.println("Database settings could not be read: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Inserts one demo account only when that username is still free.
     *
     * @param dataAccess  the store to check and write
     * @param userFactory builds and hashes the account
     * @param username    the demo username
     * @param displayName the name to show in the app
     * @param role        the role to give the account
     */
    private static void seedUser(AppDataStore dataAccess, UserFactory userFactory,
                                 String username, String displayName, Role role) {
        if (!dataAccess.existsByName(username)) {
            dataAccess.save(userFactory.create(username, DEMO_PASSWORD, displayName, role));
        }
    }

    /** Wraps a form view with breathing room so it doesn''t stretch edge to edge. */
    private static JPanel pad(JPanel inner) {
        final JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(java.awt.Color.WHITE);
        wrapper.setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));
        wrapper.add(inner, BorderLayout.NORTH);
        return wrapper;
    }

    /** Temporary page until the matching use case is implemented. */
    private static JPanel placeholder(String text) {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(java.awt.Color.WHITE);
        final JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setForeground(new java.awt.Color(130, 138, 148));
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }
}
