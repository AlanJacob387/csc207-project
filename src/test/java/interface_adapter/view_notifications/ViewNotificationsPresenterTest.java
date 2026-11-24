package interface_adapter.view_notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import use_case.view_notifications.ViewNotificationsOutputData;
import use_case.view_notifications.ViewNotificationsOutputData.NotificationData;
import use_case.view_notifications.ViewNotificationsOutputData.TeamData;

/** Verifies failed loads and session resets discard previously authorized content. */
class ViewNotificationsPresenterTest {
    @Test
    void aFailureReplacesThePreviousFeedAndTeamSelection() {
        final ViewNotificationsViewModel model = new ViewNotificationsViewModel();
        final ViewNotificationsPresenter presenter = new ViewNotificationsPresenter(model);
        final AtomicInteger updates = new AtomicInteger();
        model.addPropertyChangeListener(event -> updates.incrementAndGet());
        presenter.prepareSuccessView(sample());

        presenter.prepareFailView("The store is unavailable.");

        assertTrue(model.getTeams().isEmpty());
        assertTrue(model.getNotifications().isEmpty());
        assertEquals("", model.getSelectedTeamId());
        assertEquals("The store is unavailable.", model.getErrorMessage());
        assertFalse(model.isLoaded());
        assertEquals(2, updates.get());
    }

    @Test
    void aSessionResetClearsDataAndASubsequentReadCanReplaceIt() {
        final ViewNotificationsViewModel model = new ViewNotificationsViewModel();
        final ViewNotificationsPresenter presenter = new ViewNotificationsPresenter(model);
        presenter.prepareSuccessView(sample());

        model.clear();

        assertTrue(model.getTeams().isEmpty());
        assertTrue(model.getNotifications().isEmpty());
        assertEquals("", model.getErrorMessage());
        assertFalse(model.isLoaded());
        presenter.prepareSuccessView(new ViewNotificationsOutputData(List.of(), "", List.of()));
        assertTrue(model.isLoaded());
        assertTrue(model.getNotifications().isEmpty());
    }

    private ViewNotificationsOutputData sample() {
        return new ViewNotificationsOutputData(List.of(new TeamData("a", "Alpha")), "a",
                List.of(new NotificationData("n1", "event-a", "a", "Alpha", "CREATED", "New practice",
                        LocalDateTime.of(2026, 11, 23, 17, 0))));
    }
}
