package app;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.Team;
import entity.User;
import entity.UserFactory;
import interface_adapter.Session;
import interface_adapter.ViewManagerModel;
import interface_adapter.assign_coach.AssignCoachController;
import interface_adapter.assign_coach.AssignCoachPresenter;
import interface_adapter.assign_coach.AssignCoachViewModel;
import interface_adapter.change_password.ChangePasswordController;
import interface_adapter.change_password.ChangePasswordPresenter;
import interface_adapter.change_password.ChangePasswordViewModel;
import interface_adapter.create_event.CreateEventController;
import interface_adapter.create_event.CreateEventPresenter;
import interface_adapter.create_event.CreateEventViewModel;
import interface_adapter.create_team.CreateTeamController;
import interface_adapter.create_team.CreateTeamPresenter;
import interface_adapter.create_team.CreateTeamViewModel;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.logout.LogoutController;
import interface_adapter.logout.LogoutPresenter;
import interface_adapter.manage_roster.ManageRosterController;
import interface_adapter.manage_roster.ManageRosterPresenter;
import interface_adapter.manage_roster.ManageRosterViewModel;
import interface_adapter.post_announcement.PostAnnouncementController;
import interface_adapter.post_announcement.PostAnnouncementPresenter;
import interface_adapter.post_announcement.PostAnnouncementViewModel;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import interface_adapter.view_announcements.ViewAnnouncementsController;
import interface_adapter.view_announcements.ViewAnnouncementsPresenter;
import interface_adapter.view_announcements.ViewAnnouncementsViewModel;
import interface_adapter.view_roster.ViewRosterController;
import interface_adapter.view_roster.ViewRosterPresenter;
import interface_adapter.view_roster.ViewRosterViewModel;
import use_case.assign_coach.AssignCoachInteractor;
import use_case.change_password.ChangePasswordInteractor;
import use_case.create_event.CreateEventInteractor;
import use_case.create_team.CreateTeamInteractor;
import use_case.login.LoginInteractor;
import use_case.logout.LogoutInteractor;
import use_case.manage_roster.ManageRosterInteractor;
import use_case.post_announcement.PostAnnouncementInteractor;
import use_case.signup.SignupInteractor;
import use_case.view_announcements.ViewAnnouncementsInteractor;
import use_case.view_roster.ViewRosterInteractor;
import view.AnnouncementsView;
import view.AppShell;
import view.ChangePasswordView;
import view.CalendarView;
import view.CreateEventView;
import view.LoginView;
import view.RosterView;
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

    // Shared collaborators, created once and handed to whatever needs them.
    private final FileAppDataAccessObject dataAccess = new FileAppDataAccessObject();
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

    /** Sets up the collaborators and makes sure a fresh install is usable. */
    public AppBuilder() {
        seedDemoDataIfEmpty();
    }

    /**
     * Adds the login page.
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addLoginView() {
        loginViewModel = new LoginViewModel();
        loginView = new LoginView(loginViewModel);
        viewManager.register(ViewManager.LOGIN, loginView);
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
        session.addPropertyChangeListener(evt -> appShell.applyRole(session.getRole()));

        appShell.addPage("Calendar", new CalendarView());
        appShell.addPage("Events", placeholder("Events - TODO (event list page)"));
        appShell.addPage("My Stats",
                placeholder("My Stats - TODO (view_stats use case)"), Role.PLAYER);

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
     * Adds the Roster page with the create team, assign coach, manage roster and
     * view roster use cases. Admins and coaches see the page. Call it after addAppShell().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addRosterUseCases() {
        final CreateTeamViewModel createTeamViewModel = new CreateTeamViewModel();
        final AssignCoachViewModel assignCoachViewModel = new AssignCoachViewModel();
        final ManageRosterViewModel manageRosterViewModel = new ManageRosterViewModel();
        final ViewRosterViewModel viewRosterViewModel = new ViewRosterViewModel();

        final CreateTeamController createTeamController = new CreateTeamController(
                new CreateTeamInteractor(dataAccess, new CreateTeamPresenter(createTeamViewModel)),
                session);
        final AssignCoachController assignCoachController = new AssignCoachController(
                new AssignCoachInteractor(dataAccess, new AssignCoachPresenter(assignCoachViewModel)),
                session);
        final ManageRosterController manageRosterController = new ManageRosterController(
                new ManageRosterInteractor(dataAccess, new ManageRosterPresenter(manageRosterViewModel)),
                session);
        final ViewRosterController viewRosterController = new ViewRosterController(
                new ViewRosterInteractor(dataAccess, new ViewRosterPresenter(viewRosterViewModel)),
                session);

        appShell.addPage("Roster", new RosterView(createTeamController, createTeamViewModel,
                assignCoachController, assignCoachViewModel,
                manageRosterController, manageRosterViewModel,
                viewRosterController, viewRosterViewModel, session),
                Role.ADMIN, Role.COACH);
        return this;
    }

    /**
     * Builds the window, ready to be made visible. The login page is shown first.
     *
     * @return the application window
     */
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
                new ChangePasswordInteractor(dataAccess, presenter, session, userFactory));

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
     * only when the store is empty, so the app can be tried straight away.
     * Replace this with the team''s real seed data (task 4.4).
     */
    private void seedDemoDataIfEmpty() {
        if (dataAccess.existsByName("coach1")) {
            return;
        }
        dataAccess.save(new User("coach1", DEMO_PASSWORD, "Coach One", Role.COACH));
        dataAccess.save(new User("player1", DEMO_PASSWORD, "Pat Player", Role.PLAYER));
        dataAccess.save(new User("parent1", DEMO_PASSWORD, "Sam Parent", Role.PARENT));
        dataAccess.save(new User("admin1", DEMO_PASSWORD, "Ada Admin", Role.ADMIN));

        final Team team = new Team("demo-team", "Demo Team", "Basketball", "coach1");
        team.addPlayer("player1");
        dataAccess.saveTeam(team);
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
