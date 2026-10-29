package app;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import data_access.InMemoryEventDataAccessObject;
import interface_adapter.create_event.CreateEventController;
import interface_adapter.create_event.CreateEventPresenter;
import interface_adapter.create_event.CreateEventViewModel;
import use_case.create_event.CreateEventInteractor;
import view.AppShell;
import view.CalendarView;
import view.CreateEventView;
import view.LoginView;
import view.ViewManager;

/**
 * Application entry point: one window, all pages.
 *
 * Structure:
 *   JFrame
 *    └── ViewManager (CardLayout)
 *         ├── "login" : LoginView (full-screen, stadium background)
 *         └── "app"   : AppShell (sidebar navigation)
 *                        ├── "Schedule"  : CalendarView
 *                        ├── "New Event" : CreateEventView
 *                        └── ...placeholder pages for the team to build
 *
 * TODO: as use cases are implemented, move this wiring into an AppBuilder
 * (one add...View()/add...UseCase() method per feature, like the course's
 * user-login example).
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // --- Dependency wiring (Create Event slice) ---
            final InMemoryEventDataAccessObject dataAccess = new InMemoryEventDataAccessObject();
            dataAccess.addTeamId("demo-team"); // demo data so the slice is testable

            final CreateEventViewModel createEventViewModel = new CreateEventViewModel();
            final CreateEventPresenter createEventPresenter =
                    new CreateEventPresenter(createEventViewModel);
            final CreateEventInteractor createEventInteractor =
                    new CreateEventInteractor(dataAccess, createEventPresenter);
            final CreateEventController createEventController =
                    new CreateEventController(createEventInteractor);

            // --- Pages ---
            final ViewManager viewManager = new ViewManager();

            final LoginView loginView = new LoginView();
            final AppShell shell = new AppShell(() -> viewManager.show(ViewManager.LOGIN));

            shell.addPage("Calendar", new CalendarView());
            shell.addPage("Events", placeholder("Events — TODO (event list page)"));
            shell.addPage("New Event",
                    pad(new CreateEventView(createEventController, createEventViewModel)));
            shell.addPage("Announcements", placeholder("Announcements — TODO (notifications use case)"));
            shell.addPage("My Stats", placeholder("My Stats — TODO (view_stats use case)"));
            shell.addPage("Roster", placeholder("Roster — TODO (manage_teams use case)"));

            loginView.setOnLoginSuccess(() -> viewManager.show(ViewManager.APP));

            viewManager.register(ViewManager.LOGIN, loginView);
            viewManager.register(ViewManager.APP, shell);
            viewManager.show(ViewManager.LOGIN);

            // --- Window ---
            final JFrame frame = new JFrame("Team Sports Management Platform");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(viewManager);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setMinimumSize(new Dimension(1000, 700));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    /** Wraps a form view with breathing room so it doesn't stretch edge to edge. */
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