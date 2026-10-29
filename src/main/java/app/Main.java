package app;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.Team;
import entity.User;
import entity.UserFactory;
import interface_adapter.Session;
import interface_adapter.ViewManagerModel;
import interface_adapter.create_event.CreateEventController;
import interface_adapter.create_event.CreateEventPresenter;
import interface_adapter.create_event.CreateEventViewModel;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import use_case.create_event.CreateEventInteractor;
import use_case.login.LoginInteractor;
import use_case.signup.SignupInteractor;
import view.AppShell;
import view.CalendarView;
import view.CreateEventView;
import view.LoginView;
import view.SignupView;
import view.ViewManager;

/**
 * Application entry point: one window, all pages.
 *
 * Structure:
 *   JFrame
 *    +-- ViewManager (CardLayout, driven by ViewManagerModel)
 *         +-- "login"  : LoginView  (full-screen, stadium background)
 *         +-- "signup" : SignupView
 *         +-- "app"    : AppShell   (sidebar navigation, filtered by role)
 *                          +-- "Calendar"      : CalendarView     (everyone)
 *                          +-- "Events"        : placeholder      (everyone)
 *                          +-- "New Event"     : CreateEventView  (coaches)
 *                          +-- "Announcements" : placeholder      (everyone)
 *                          +-- "My Stats"      : placeholder      (players)
 *                          +-- "Roster"        : placeholder      (admins, coaches)
 *
 * TODO: as more use cases land, move this wiring into an AppBuilder with one
 * add...View()/add...UseCase() method per feature, like the course example.
 */
public class Main {

    /** Password given to the demo accounts seeded below. */
    private static final String DEMO_PASSWORD = "pw";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // --- data access ---
            final FileAppDataAccessObject dataAccess = new FileAppDataAccessObject();
            seedDemoData(dataAccess);

            // --- shared state ---
            final ViewManagerModel viewManagerModel = new ViewManagerModel();
            final Session session = new Session();
            final UserFactory userFactory = new UserFactory();

            // --- log in ---
            final LoginViewModel loginViewModel = new LoginViewModel();
            final LoginPresenter loginPresenter =
                    new LoginPresenter(viewManagerModel, loginViewModel, session);
            final LoginController loginController =
                    new LoginController(new LoginInteractor(dataAccess, loginPresenter));

            // --- sign up ---
            final SignupViewModel signupViewModel = new SignupViewModel();
            final SignupPresenter signupPresenter =
                    new SignupPresenter(viewManagerModel, signupViewModel, loginViewModel);
            final SignupController signupController = new SignupController(
                    new SignupInteractor(dataAccess, signupPresenter, userFactory));

            // --- create event (existing slice) ---
            final CreateEventViewModel createEventViewModel = new CreateEventViewModel();
            final CreateEventPresenter createEventPresenter =
                    new CreateEventPresenter(createEventViewModel);
            final CreateEventController createEventController = new CreateEventController(
                    new CreateEventInteractor(dataAccess, createEventPresenter));

            // --- pages ---
            final ViewManager viewManager = new ViewManager(viewManagerModel);

            final LoginView loginView = new LoginView(loginViewModel);
            loginView.setLoginController(loginController);
            loginView.setOnGoToSignup(() -> navigate(viewManagerModel, ViewManagerModel.SIGNUP));

            final SignupView signupView = new SignupView(signupViewModel);
            signupView.setSignupController(signupController);
            signupView.setOnBackToLogin(() -> navigate(viewManagerModel, ViewManagerModel.LOGIN));

            final AppShell shell = new AppShell(() -> {
                session.logOut();
                navigate(viewManagerModel, ViewManagerModel.LOGIN);
            });
            session.addPropertyChangeListener(evt -> shell.applyRole(session.getRole()));

            shell.addPage("Calendar", new CalendarView());
            shell.addPage("Events", placeholder("Events - TODO (event list page)"));
            shell.addPage("New Event",
                    pad(new CreateEventView(createEventController, createEventViewModel)),
                    Role.COACH);
            shell.addPage("Announcements",
                    placeholder("Announcements - TODO (notifications use case)"));
            shell.addPage("My Stats",
                    placeholder("My Stats - TODO (view_stats use case)"), Role.PLAYER);
            shell.addPage("Roster",
                    placeholder("Roster - TODO (manage_teams use case)"),
                    Role.ADMIN, Role.COACH);

            viewManager.register(ViewManager.LOGIN, loginView);
            viewManager.register(ViewManager.SIGNUP, signupView);
            viewManager.register(ViewManager.APP, shell);
            viewManager.show(ViewManager.LOGIN);

            // --- window ---
            final JFrame frame = new JFrame("Team Sports Management Platform");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(viewManager);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setMinimumSize(new Dimension(1000, 700));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /**
     * Asks the view manager to show a page. Presenters do the same thing, which
     * is how a successful login switches to the signed-in shell.
     *
     * @param viewManagerModel holds the page that should be visible
     * @param page             one of the page-name constants
     */
    private static void navigate(ViewManagerModel viewManagerModel, String page) {
        viewManagerModel.setState(page);
        viewManagerModel.firePropertyChanged();
    }

    /**
     * Creates one demo account per role (password "pw") and a demo team, but
     * only when the store is empty, so the app is usable straight away.
     * Replace this with the team''s real seed data.
     *
     * @param dataAccess the store to seed
     */
    private static void seedDemoData(FileAppDataAccessObject dataAccess) {
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