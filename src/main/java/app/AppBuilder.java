package app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import data_access.AppDataStore;
import data_access.DatabaseConfig;
import data_access.DatabaseSettingsGateway;
import data_access.FileAppDataAccessObject;
import data_access.FileScheduleWriter;
import data_access.OpenMeteoWeatherGateway;
import data_access.SupabaseAppDataAccessObject;
import entity.Role;
import entity.Team;
import entity.User;
import entity.UserFactory;
import interface_adapter.Session;
import interface_adapter.ViewManagerModel;
import interface_adapter.view_notifications.ViewNotificationsController;
import interface_adapter.view_notifications.ViewNotificationsPresenter;
import interface_adapter.view_notifications.ViewNotificationsViewModel;
import interface_adapter.submit_absence_notice.SubmitAbsenceNoticeController;
import interface_adapter.submit_absence_notice.SubmitAbsenceNoticePresenter;
import interface_adapter.submit_absence_notice.SubmitAbsenceNoticeViewModel;
import interface_adapter.view_absence_notices.ViewAbsenceNoticesController;
import interface_adapter.view_absence_notices.ViewAbsenceNoticesPresenter;
import interface_adapter.view_absence_notices.ViewAbsenceNoticesViewModel;
import interface_adapter.link_parent_to_player.LinkParentToPlayerController;
import interface_adapter.link_parent_to_player.LinkParentToPlayerPresenter;
import interface_adapter.link_parent_to_player.LinkParentToPlayerViewModel;
import interface_adapter.view_event_weather.ViewEventWeatherController;
import interface_adapter.view_event_weather.ViewEventWeatherViewModel;
import interface_adapter.flag_rain_risk.FlagRainRiskController;
import interface_adapter.flag_rain_risk.FlagRainRiskViewModel;
import interface_adapter.browse_events.BrowseEventsController;
import interface_adapter.browse_events.BrowseEventsPresenter;
import interface_adapter.browse_events.BrowseEventsViewModel;
import interface_adapter.cancel_event.CancelEventController;
import interface_adapter.cancel_event.CancelEventPresenter;
import interface_adapter.cancel_event.CancelEventViewModel;
import interface_adapter.change_password.ChangePasswordController;
import interface_adapter.change_password.ChangePasswordPresenter;
import interface_adapter.change_password.ChangePasswordViewModel;
import interface_adapter.create_event.CreateEventController;
import interface_adapter.database_settings.DatabaseSettingsController;
import interface_adapter.database_settings.DatabaseSettingsPresenter;
import interface_adapter.database_settings.DatabaseSettingsViewModel;
import interface_adapter.create_event.CreateEventPresenter;
import interface_adapter.create_event.CreateEventViewModel;
import interface_adapter.edit_event.EditEventController;
import interface_adapter.edit_event.EditEventPresenter;
import interface_adapter.edit_event.EditEventViewModel;
import interface_adapter.export_schedule.ExportScheduleController;
import interface_adapter.export_schedule.ExportSchedulePresenter;
import interface_adapter.export_schedule.ExportScheduleViewModel;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.logout.LogoutController;
import interface_adapter.logout.LogoutPresenter;
import interface_adapter.manage_users.ChangeUserRoleController;
import interface_adapter.manage_users.CreateUserController;
import interface_adapter.manage_users.DeactivateUserController;
import interface_adapter.manage_users.ListAccountsController;
import interface_adapter.manage_users.ManageUsersPresenter;
import interface_adapter.manage_users.ManageUsersViewModel;
import interface_adapter.manage_users.ReactivateUserController;
import interface_adapter.post_announcement.PostAnnouncementController;
import interface_adapter.post_announcement.PostAnnouncementPresenter;
import interface_adapter.post_announcement.PostAnnouncementViewModel;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import interface_adapter.view_announcements.ViewAnnouncementsController;
import interface_adapter.view_announcements.ViewAnnouncementsPresenter;
import interface_adapter.view_announcements.ViewAnnouncementsViewModel;
import use_case.accounts.DuplicateUsernameException;
import interface_adapter.view_event_details.ViewEventDetailsController;
import interface_adapter.view_event_details.ViewEventDetailsPresenter;
import interface_adapter.view_event_details.ViewEventDetailsViewModel;
import interface_adapter.view_schedule.ViewScheduleController;
import interface_adapter.view_schedule.ViewSchedulePresenter;
import interface_adapter.view_schedule.ViewScheduleViewModel;
import use_case.browse_events.BrowseEventsInteractor;
import use_case.cancel_event.CancelEventInteractor;
import use_case.change_password.ChangePasswordInteractor;
import use_case.create_event.CreateEventInteractor;
import use_case.edit_event.EditEventInteractor;
import use_case.export_schedule.ExportScheduleInteractor;
import use_case.database_settings.DatabaseSettingsInteractor;
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
import use_case.view_event_details.ViewEventDetailsInteractor;
import use_case.view_schedule.ViewScheduleInteractor;
import use_case.view_notifications.ViewNotificationsInteractor;
import use_case.submit_absence_notice.SubmitAbsenceNoticeInteractor;
import use_case.view_absence_notices.ViewAbsenceNoticesInteractor;
import use_case.link_parent_to_player.LinkParentToPlayerInteractor;
import view.AnnouncementsView;
import view.AppShell;
import view.ChangePasswordView;
import view.CalendarView;
import view.CreateEventView;
import view.DatabaseSettingsView;
import view.EventDetailsView;
import view.EventsListView;
import view.LoginView;
import view.ManageUsersView;
import view.SignupView;
import view.ViewManager;
import view.NotificationsView;
import view.AbsenceNoticesView;
import view.ParentLinksView;
import view.EventWeatherPanel;
import view.RainRiskPanel;

/**
 * Builds the whole application through a chain of add... methods, so Main
 * just states what the app has.
 *
 * Not every page has its own add...View() method. Only the login, sign-up and
 * account pages do. addAppShell() also builds the calendar and events list,
 * and some use case methods build the view they need: addCreateEventUseCase()
 * builds the New Event page, addViewEventDetailsUseCase() builds the event
 * details panel, and addAnnouncementUseCases() wires two use cases and the
 * announcements page together. This mirrors the AppBuilder in the course''s
 * user-login example.
 *
 * Typical order: the login and sign-up views, then the shell, then the use
 * cases and remaining views, then build(). Methods that read a field set by
 * an earlier method must come after it.
 */
public class AppBuilder {

    /** Password given to the demo accounts seeded on first run in a hosted store. */
    private static final String DEMO_PASSWORD = "teampass";

    /** Id of the demo team seeded on first run in a hosted store. */
    private static final String DEMO_TEAM_ID = "demo-team";

    /** The usernames the demo seed owns; any other account means the store is in real use. */
    private static final Set<String> DEMO_USERNAMES =
            Set.of("coach1", "player1", "parent1", "admin1");

    // Shared collaborators, created once and handed to whatever needs them.
    private final DatabaseConfig databaseConfig;
    private final AppDataStore dataAccess;
    private final ViewManagerModel viewManagerModel = new ViewManagerModel();
    private final Session session = new Session();
    private final UserFactory userFactory = new UserFactory();
    private final ViewManager viewManager = new ViewManager(viewManagerModel);

    private LoginView loginView;
    private LoginViewModel loginViewModel;
    private SignupView signupView;
    private SignupViewModel signupViewModel;
    private AppShell appShell;
    private CalendarView calendarView;
    private EventDetailsView eventDetailsView;
    private EventsListView eventsListView;
    private CreateEventViewModel createEventViewModel;
    private ChangePasswordView changePasswordView;
    private ChangePasswordViewModel changePasswordViewModel;

    /** What went wrong before any page could open, such as an unreachable store. */
    private String startupNotice = "";

    /** Builds against the store the user's settings choose: the hosted database or the local file. */
    public AppBuilder() {
        this(resolveDatabaseConfig());
    }

    private AppBuilder(DatabaseConfig config) {
        this(config, createStore(config));
    }

    /** Builds against an explicit store so demonstrations and tests can use isolated data. */
    public AppBuilder(AppDataStore dataAccess) {
        this(null, dataAccess);
    }

    private AppBuilder(DatabaseConfig config, AppDataStore dataAccess) {
        this.databaseConfig = config;
        this.dataAccess = Objects.requireNonNull(dataAccess);
        startupNotice = seedDemoData(this.dataAccess, userFactory);
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
        final DatabaseSettingsViewModel viewModel = new DatabaseSettingsViewModel();
        final DatabaseSettingsPresenter presenter = new DatabaseSettingsPresenter(viewModel);
        final DatabaseSettingsController controller = new DatabaseSettingsController(
                new DatabaseSettingsInteractor(new DatabaseSettingsGateway(), presenter));

        final DatabaseSettingsView view = new DatabaseSettingsView(viewModel);
        view.setController(controller);
        view.setOnBackToLogin(() -> navigate(ViewManagerModel.LOGIN));
        controller.prepareView();
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

        calendarView = new CalendarView();
        appShell.addPage("Calendar", calendarView);
        eventsListView = new EventsListView();
        appShell.addPage("Events", eventsListView);
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
                new CreateEventInteractor(dataAccess, presenter, session));

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

    /** Adds the durable feed of changes to the current user's teams' events. */
    public AppBuilder addNotificationUseCase() {
        final ViewNotificationsViewModel model = new ViewNotificationsViewModel();
        final ViewNotificationsController controller = new ViewNotificationsController(
                new ViewNotificationsInteractor(dataAccess, new ViewNotificationsPresenter(model), session));
        final NotificationsView view = new NotificationsView(controller, model);
        session.addPropertyChangeListener(event -> view.reset());
        appShell.addPage("Notifications", view);
        return this;
    }

    /** Adds linked-child absence submission and the coach's notice list. */
    public AppBuilder addAbsenceNoticeUseCases() {
        final SubmitAbsenceNoticeViewModel submitModel = new SubmitAbsenceNoticeViewModel();
        final ViewAbsenceNoticesViewModel listModel = new ViewAbsenceNoticesViewModel();
        final SubmitAbsenceNoticeController submitController = new SubmitAbsenceNoticeController(
                new SubmitAbsenceNoticeInteractor(dataAccess, new SubmitAbsenceNoticePresenter(submitModel), session));
        final ViewAbsenceNoticesController listController = new ViewAbsenceNoticesController(
                new ViewAbsenceNoticesInteractor(dataAccess, new ViewAbsenceNoticesPresenter(listModel), session));
        appShell.addPage("Absence notices", new AbsenceNoticesView(submitController, submitModel,
                listController, listModel, session), Role.PARENT, Role.COACH);
        return this;
    }

    /** Lets an administrator grant and revoke access between existing parents and players. */
    public AppBuilder addParentLinksUseCase() {
        final LinkParentToPlayerViewModel model = new LinkParentToPlayerViewModel();
        final LinkParentToPlayerController controller = new LinkParentToPlayerController(
                new LinkParentToPlayerInteractor(dataAccess, new LinkParentToPlayerPresenter(model), session));
        appShell.addPage("Parent links", new ParentLinksView(controller, model, session), Role.ADMIN);
        return this;
    }

    /** Connects forecasts to event details and rain-risk labels to the calendar and list. */
    public AppBuilder addWeatherUseCases() {
        final OpenMeteoWeatherGateway gateway = new OpenMeteoWeatherGateway();
        final ViewEventWeatherViewModel eventModel = new ViewEventWeatherViewModel();
        final EventWeatherPanel weather = new EventWeatherPanel(
                new ViewEventWeatherController(dataAccess, gateway, eventModel, session), eventModel, session);
        eventDetailsView.setWeatherPanel(weather);

        final FlagRainRiskViewModel riskModel = new FlagRainRiskViewModel();
        final RainRiskPanel risks = new RainRiskPanel(
                new FlagRainRiskController(dataAccess, gateway, riskModel, session), riskModel, session);
        risks.setOnFlags(flags -> {
            calendarView.setRainRiskFlags(flags);
            eventsListView.setRainRiskFlags(flags);
        });
        eventDetailsView.setOnEventChanged(risks::clear);
        createEventViewModel.addPropertyChangeListener(event -> {
            if (!createEventViewModel.getSuccessMessage().isEmpty()) {
                risks.clear();
            }
        });
        appShell.addPage("Weather", risks);
        return this;
    }

    /**
     * Wires the View Schedule use case into the calendar page, so it shows the
     * signed-in user's stored events. Call it after addAppShell().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addViewScheduleUseCase() {
        final ViewScheduleViewModel viewModel = new ViewScheduleViewModel();
        final ViewSchedulePresenter presenter = new ViewSchedulePresenter(viewModel);
        final ViewScheduleController controller = new ViewScheduleController(
                new ViewScheduleInteractor(dataAccess, presenter, session));

        calendarView.setViewScheduleController(controller, viewModel);
        return this;
    }

    /**
     * Wires the View Event Details use case: clicking an event on the calendar
     * opens it in a dialog, and the calendar reloads when the dialog closes.
     * Call it after addAppShell().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addViewEventDetailsUseCase() {
        final ViewEventDetailsViewModel viewModel = new ViewEventDetailsViewModel();
        final ViewEventDetailsPresenter presenter = new ViewEventDetailsPresenter(viewModel);
        final ViewEventDetailsController controller = new ViewEventDetailsController(
                new ViewEventDetailsInteractor(dataAccess, presenter, session));

        eventDetailsView = new EventDetailsView(controller, viewModel);
        eventDetailsView.setOnClose(calendarView::refresh);
        calendarView.setOnOpenEvent(eventId -> eventDetailsView.open(eventId, calendarView));
        return this;
    }

    /**
     * Wires the Edit Event use case into the event details dialog.
     * Call it after addViewEventDetailsUseCase().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addEditEventUseCase() {
        final EditEventViewModel viewModel = new EditEventViewModel();
        final EditEventPresenter presenter = new EditEventPresenter(viewModel);
        final EditEventController controller = new EditEventController(
                new EditEventInteractor(dataAccess, presenter, session));

        eventDetailsView.setEditEventController(controller, viewModel);
        return this;
    }

    /**
     * Wires the Cancel Event use case into the event details dialog.
     * Call it after addViewEventDetailsUseCase().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addCancelEventUseCase() {
        final CancelEventViewModel viewModel = new CancelEventViewModel();
        final CancelEventPresenter presenter = new CancelEventPresenter(viewModel);
        final CancelEventController controller = new CancelEventController(
                new CancelEventInteractor(dataAccess, presenter, session));

        eventDetailsView.setCancelEventController(controller, viewModel);
        return this;
    }

    /**
     * Wires the Browse Events use case into the Events page. Clicking a row
     * opens the same details dialog the calendar uses, and the list reloads
     * once it closes. Call it after addViewEventDetailsUseCase().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addBrowseEventsUseCase() {
        final BrowseEventsViewModel viewModel = new BrowseEventsViewModel();
        final BrowseEventsPresenter presenter = new BrowseEventsPresenter(viewModel);
        final BrowseEventsController controller = new BrowseEventsController(
                new BrowseEventsInteractor(dataAccess, presenter, session));

        eventsListView.setBrowseEventsController(controller, viewModel);
        // open() is modal and returns once the dialog is closed.
        eventsListView.setOnOpenEvent(eventId -> {
            eventDetailsView.open(eventId, eventsListView);
            eventsListView.refresh();
        });
        return this;
    }

    /**
     * Wires the Export Schedule use case into the calendar's Export button.
     * Call it after addAppShell().
     *
     * @return this builder, so the calls can be chained
     */
    public AppBuilder addExportScheduleUseCase() {
        final ExportScheduleViewModel viewModel = new ExportScheduleViewModel();
        final ExportSchedulePresenter presenter = new ExportSchedulePresenter(viewModel);
        final ExportScheduleController controller = new ExportScheduleController(
                new ExportScheduleInteractor(dataAccess, new FileScheduleWriter(),
                        presenter, session));

        calendarView.setExportScheduleController(controller, viewModel);
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

        appShell.addPage("Manage Users", new ManageUsersView(
                        new ListAccountsController(
                                new ListAccountsInteractor(dataAccess, presenter, session)),
                        new CreateUserController(
                                new CreateUserInteractor(dataAccess, presenter, session,
                                        userFactory)),
                        new ChangeUserRoleController(
                                new ChangeUserRoleInteractor(dataAccess, presenter, session)),
                        new DeactivateUserController(
                                new DeactivateUserInteractor(dataAccess, presenter, session)),
                        new ReactivateUserController(
                                new ReactivateUserInteractor(dataAccess, presenter, session)),
                        viewModel, session),
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

    /**
     * Builds the window, ready to be made visible. The login page is shown first.
     *
     * @return the application window
     */
    public JFrame build() {
        final JFrame frame = new JFrame("Team Sports Management Platform");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(buildContent());
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        frame.setMinimumSize(new Dimension(1000, 700));
        frame.setLocationRelativeTo(null);

        return frame;
    }

    /** Returns the same application content for a frame or an offscreen flow check. */
    public JPanel buildContent() {
        navigate(ViewManagerModel.LOGIN);
        return viewManager;
    }

    /** Exposes the modal's content to application-level offscreen flow checks. */
    EventDetailsView getEventDetailsView() {
        return eventDetailsView;
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
     * Makes a fresh install usable straight away, without ever replacing data.
     *
     * The local file gets the full presentation dataset ({@link DemoDataSeeder}),
     * but only while it is empty. A hosted store is shared, so it only gets the
     * four demo accounts and a team, each inserted where it is missing. Either
     * way a store that cannot be reached becomes a message for the sign-in page.
     *
     * @param dataAccess  the store the whole app will use
     * @param userFactory builds and hashes the hosted demo accounts
     * @return an empty string on success, or what the sign-in page should say
     */
    static String seedDemoData(AppDataStore dataAccess, UserFactory userFactory) {
        if (dataAccess instanceof FileAppDataAccessObject) {
            try {
                DemoDataSeeder.seedIfEmpty((FileAppDataAccessObject) dataAccess, Clock.systemDefaultZone());
            }
            catch (IllegalStateException | UncheckedIOException ex) {
                return "Could not write the demo data: " + ex.getMessage();
            }
            return "";
        }
        return seedDemoDataIfEmpty(dataAccess, userFactory);
    }

    /**
     * Creates one demo account per role (password "teampass") and a demo team, but
     * only while the store holds no account of its own.
     *
     * "Of its own" means any account whose username is not one of the four demo
     * names. Once a real account exists the seed does nothing, so a demo account
     * an administrator deleted (above all {@code admin1}, which has a published
     * password) stays deleted instead of coming back on the next start of any
     * client. A store that holds only demo accounts - a first run, or one that
     * stopped halfway - is completed, and an account that is already there is
     * kept as it is.
     *
     * Reports a failure instead of throwing: a store that cannot be reached while
     * the app is starting must still leave the user on the sign-in page, where the
     * reason is shown and the database settings can be corrected. Nothing is
     * swapped for a local file - the caller keeps the store it chose.
     *
     * Each account is inserted, never upserted, so a name another client created
     * a moment earlier is not overwritten. The factory hashes each demo password,
     * so a fresh store never holds plaintext credentials. Replace this with the
     * team's real seed data (task 4.4).
     *
     * @param dataAccess  the store the whole app will use
     * @param userFactory builds and hashes the demo accounts
     * @return an empty string on success, or what the sign-in page should say
     */
    static String seedDemoDataIfEmpty(AppDataStore dataAccess, UserFactory userFactory) {
        try {
            if (holdsAnAccountOfItsOwn(dataAccess)) {
                return "";
            }
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
     * Says whether any stored account is not one of the demo accounts.
     *
     * @param dataAccess the store to read
     * @return true when the store is already in real use
     */
    private static boolean holdsAnAccountOfItsOwn(AppDataStore dataAccess) {
        for (User user : dataAccess.getAllUsers()) {
            if (!DEMO_USERNAMES.contains(user.getUsername())) {
                return true;
            }
        }
        return false;
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
                ? new FileAppDataAccessObject(
                        Path.of(System.getProperty("teamsports.data", "data/app-data.json")))
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
     * Inserts one demo account, leaving one that is already stored untouched.
     *
     * @param dataAccess  the store to write
     * @param userFactory builds and hashes the account
     * @param username    the demo username
     * @param displayName the name to show in the app
     * @param role        the role to give the account
     */
    private static void seedUser(AppDataStore dataAccess, UserFactory userFactory,
                                 String username, String displayName, Role role) {
        try {
            dataAccess.createUser(userFactory.create(username, DEMO_PASSWORD, displayName, role));
        }
        catch (DuplicateUsernameException ex) {
            // Already there, from an earlier start or another client: keep it as it is.
            return;
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
