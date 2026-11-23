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
import interface_adapter.record_attendance.RecordAttendanceController;
import interface_adapter.record_attendance.RecordAttendancePresenter;
import interface_adapter.record_attendance.RecordAttendanceViewModel;
import use_case.record_attendance.RecordAttendanceInteractor;
import use_case.current_user.CurrentUserInterface;
import view.RecordAttendanceView;
import view.RsvpView;
import view.EditAttendanceView;
import view.MyStatsView;
import interface_adapter.rsvp.*;
import interface_adapter.edit_attendance.*;
import interface_adapter.view_attendance_history.*;
import interface_adapter.view_stats.*;
import use_case.rsvp.*;
import use_case.edit_attendance.*;
import use_case.view_attendance_history.*;
import use_case.view_stats.*;
import use_case.list_available_events.*;
import interface_adapter.list_available_events.*;
import interface_adapter.ViewManagerModel;
import interface_adapter.create_event.CreateEventController;
import interface_adapter.create_event.CreateEventPresenter;
import interface_adapter.create_event.CreateEventViewModel;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.post_announcement.PostAnnouncementController;
import interface_adapter.post_announcement.PostAnnouncementPresenter;
import interface_adapter.post_announcement.PostAnnouncementViewModel;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import interface_adapter.view_announcements.ViewAnnouncementsController;
import interface_adapter.view_announcements.ViewAnnouncementsPresenter;
import interface_adapter.view_announcements.ViewAnnouncementsViewModel;
import use_case.create_event.CreateEventInteractor;
import use_case.login.LoginInteractor;
import use_case.post_announcement.PostAnnouncementInteractor;
import use_case.signup.SignupInteractor;
import use_case.view_announcements.ViewAnnouncementsInteractor;
import view.AnnouncementsView;
import view.AppShell;
import view.CalendarView;
import view.CreateEventView;
import view.LoginView;
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
        appShell = new AppShell(() -> {
            session.logOut();
            navigate(ViewManagerModel.LOGIN);
        });
        session.addPropertyChangeListener(evt -> appShell.applyRole(session.getRole()));

        appShell.addPage("Calendar", new CalendarView());
        appShell.addPage("Events", placeholder("Events - TODO (event list page)"));
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
                new LoginPresenter(viewManagerModel, loginViewModel, session);
        final LoginController controller =
                new LoginController(new LoginInteractor(dataAccess, presenter));

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

    /** Wires the coach-only complete-roster attendance page. */
    public AppBuilder addRecordAttendanceUseCase() {
        final RecordAttendanceViewModel vm = new RecordAttendanceViewModel();
        final RecordAttendanceController controller = new RecordAttendanceController(
                new RecordAttendanceInteractor(dataAccess, new RecordAttendancePresenter(vm), session));
        final ListAvailableEventsViewModel eventVm = new ListAvailableEventsViewModel();
        final ListAvailableEventsController listController = new ListAvailableEventsController(
                new ListAvailableEventsInteractor(dataAccess,
                        new ListAvailableEventsPresenter(eventVm), session));
        appShell.addPage("Attendance", pad(new RecordAttendanceView(controller, vm, session,
                listController, eventVm)),
                Role.COACH);
        return this;
    }

    /** Adds event RSVPs and coach read-only response list. */
    public AppBuilder addRsvpUseCase() {
        final RsvpViewModel vm = new RsvpViewModel();
        final RsvpController controller = new RsvpController(
                new RsvpInteractor(dataAccess, new RsvpPresenter(vm), session));
        final ListAvailableEventsViewModel eventVm = new ListAvailableEventsViewModel();
        final ListAvailableEventsController listController = new ListAvailableEventsController(
                new ListAvailableEventsInteractor(dataAccess,
                        new ListAvailableEventsPresenter(eventVm), session));
        appShell.addPage("RSVP", pad(new RsvpView(controller, vm, session, listController, eventVm)),
                Role.PLAYER, Role.COACH);
        return this;
    }

    /** Adds the coach-only attendance correction form. */
    public AppBuilder addEditAttendanceUseCase() {
        final EditAttendanceViewModel vm = new EditAttendanceViewModel();
        final EditAttendanceController controller = new EditAttendanceController(
                new EditAttendanceInteractor(dataAccess, new EditAttendancePresenter(vm), session));
        final ListAvailableEventsViewModel eventVm = new ListAvailableEventsViewModel();
        final ListAvailableEventsController listController = new ListAvailableEventsController(
                new ListAvailableEventsInteractor(dataAccess,
                        new ListAvailableEventsPresenter(eventVm), session));
        appShell.addPage("Edit Attendance", pad(new EditAttendanceView(controller, vm, session,
                listController, eventVm)),
                Role.COACH);
        return this;
    }

    /** Adds player-only history and summary, each backed by a separate use case. */
    public AppBuilder addMyStatsUseCases() {
        final ViewAttendanceHistoryViewModel history = new ViewAttendanceHistoryViewModel();
        final ViewStatsViewModel stats = new ViewStatsViewModel();
        final ViewAttendanceHistoryController historyController = new ViewAttendanceHistoryController(
                new ViewAttendanceHistoryInteractor(dataAccess,
                        new ViewAttendanceHistoryPresenter(history), session));
        final ViewStatsController statsController = new ViewStatsController(
                new ViewStatsInteractor(dataAccess, new ViewStatsPresenter(stats), session));
        appShell.addPage("My Stats", new MyStatsView(historyController, history,
                statsController, stats, session), Role.PLAYER);
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
     * Builds the window, ready to be made visible. The login page is shown first.
     *
     * @return the application window
     */
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
