package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import interface_adapter.view_notifications.ViewNotificationsController;
import interface_adapter.view_notifications.ViewNotificationsPresenter;
import interface_adapter.view_notifications.ViewNotificationsViewModel;
import use_case.view_notifications.ViewNotificationsOutputData;
import use_case.view_notifications.ViewNotificationsOutputData.NotificationData;
import use_case.view_notifications.ViewNotificationsOutputData.TeamData;

/** Headless checks of filtering, refresh, readable feed content and clearing old sessions. */
class NotificationsViewTest {
    @Test
    void theFeedShowsCapturedEventTextAndReadableChangeTypesWithoutRenderingHtml() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final ViewNotificationsViewModel model = new ViewNotificationsViewModel();
            final NotificationsView view = new NotificationsView(new ViewNotificationsController(input -> { }), model);
            final ViewNotificationsPresenter presenter = new ViewNotificationsPresenter(model);
            presenter.prepareSuccessView(new ViewNotificationsOutputData(List.of(new TeamData("a", "Alpha")), "",
                    List.of(row("CREATED", "Practice on November 24 at North Field"),
                            row("CHANGED", "Game moved to 7 pm"),
                            row("CANCELLED", "<html>Practice cancelled because of rain"))));

            final JTextArea feed = (JTextArea) named(view, "notificationFeed");
            assertTrue(feed.getText().contains("Alpha  ·  Event added"));
            assertTrue(feed.getText().contains("Event changed"));
            assertTrue(feed.getText().contains("Event cancelled"));
            final String expectedTime = LocalDateTime.of(2026, 11, 23, 17, 0)
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
            assertTrue(feed.getText().contains(expectedTime));
            assertTrue(feed.getText().contains("Practice on November 24 at North Field"));
            assertTrue(feed.getText().contains("<html>Practice cancelled"));
            assertTrue(feed.getLineWrap());
            assertFalse(feed.isEditable());
        });
    }

    @Test
    void selectionAndRefreshSendOnlyTheTeamFilterToTheController() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final List<String> requests = new ArrayList<>();
            final ViewNotificationsViewModel model = new ViewNotificationsViewModel();
            final NotificationsView view = new NotificationsView(new ViewNotificationsController(
                    input -> requests.add(input.getTeamId())), model);
            model.showNotifications(new ViewNotificationsOutputData(
                    List.of(new TeamData("a", "Alpha"), new TeamData("b", "Beta")), "", List.of()));

            final JComboBox<?> teams = (JComboBox<?>) named(view, "notificationTeam");
            teams.setSelectedIndex(2);
            ((JButton) named(view, "refreshNotifications")).doClick();

            assertEquals(List.of("b", "b"), requests);
        });
    }

    @Test
    void sessionResetAndReadFailureRemoveThePreviousAccountsFeedAndFilter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final ViewNotificationsViewModel model = new ViewNotificationsViewModel();
            final NotificationsView view = new NotificationsView(new ViewNotificationsController(input -> { }), model);
            model.showNotifications(new ViewNotificationsOutputData(List.of(new TeamData("a", "Alpha")), "a",
                    List.of(row("CREATED", "Private team event"))));
            final JTextArea feed = (JTextArea) named(view, "notificationFeed");
            final JComboBox<?> teams = (JComboBox<?>) named(view, "notificationTeam");
            assertTrue(feed.getText().contains("Private team event"));

            view.reset();

            assertFalse(feed.getText().contains("Private team event"));
            assertEquals(1, teams.getItemCount());
            assertFalse(teams.isEnabled());
            model.showError("Sign in to view notifications.");
            assertEquals("", feed.getText());
            assertEquals(1, teams.getItemCount());
            model.showNotifications(new ViewNotificationsOutputData(List.of(new TeamData("b", "Beta")), "", List.of()));
            assertTrue(feed.getText().contains("No event changes"));
            assertFalse(feed.getText().contains("Private team event"));
        });
    }

    private static NotificationData row(String type, String message) {
        return new NotificationData("notice-" + type, "event-a", "a", "Alpha", type, message,
                LocalDateTime.of(2026, 11, 23, 17, 0));
    }

    private static Component named(Container parent, String name) {
        for (Component component : parent.getComponents()) {
            if (name.equals(component.getName())) {
                return component;
            }
            if (component instanceof Container container) {
                final Component child = named(container, name);
                if (child != null) {
                    return child;
                }
            }
        }
        return null;
    }
}
