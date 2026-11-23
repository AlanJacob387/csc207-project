package app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import data_access.FileAppDataAccessObject;
import data_access.FileScheduleWriter;
import data_access.OpenMeteoWeatherGateway;
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
import interface_adapter.post_announcement.PostAnnouncementController;
import interface_adapter.post_announcement.PostAnnouncementPresenter;
import interface_adapter.post_announcement.PostAnnouncementViewModel;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import interface_adapter.view_announcements.ViewAnnouncementsController;
import interface_adapter.view_announcements.ViewAnnouncementsPresenter;
import interface_adapter.view_announcements.ViewAnnouncementsViewModel;
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
import use_case.login.LoginInteractor;
import use_case.logout.LogoutInteractor;
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
import view.EventDetailsView;
import view.EventsListView;
import view.LoginView;
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

    // Shared collaborators, created once and handed to whatever needs them.
    private final FileAppDataAccessObject dataAccess;
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

    /** Sets up the collaborators and makes sure a fresh install is usable. */
    public AppBuilder() {
        this(new FileAppDataAccessObject(Path.of(System.getProperty("teamsports.data", "data/app-data.json"))));
    }

    /** Builds against an explicit store so demonstrations and tests can use isolated data. */
    public AppBuilder(FileAppDataAccessObject dataAccess) {
        this.dataAccess = Objects.requireNonNull(dataAccess);
        DemoDataSeeder.seedIfEmpty(dataAccess, java.time.Clock.systemDefaultZone());
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
