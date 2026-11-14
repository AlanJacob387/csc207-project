package use_case.cancel_event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Event;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Tests the Cancel Event use case: a successful cancel and every way it can
 * fail, including a failed save.
 */
class CancelEventInteractorTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 21, 20, 0);

    private FakeCancelEventDataAccess dataAccess;
    private FakeCurrentUser currentUser;
    private RecordingPresenter presenter;
    private CancelEventInteractor interactor;
    private Event game;

    /** Events and coaches held in memory, scoped to what this use case needs. */
    private static class FakeCancelEventDataAccess implements CancelEventDataAccessInterface {

        private final Map<String, Event> events = new HashMap<>();
        private final Map<String, String> coaches = new HashMap<>();
        private boolean failWrites;

        @Override
        public Event getEvent(String eventId) {
            return events.get(eventId);
        }

        @Override
        public String getCoachUsername(String teamId) {
            return coaches.get(teamId);
        }

        @Override
        public void updateEvent(Event updated) {
            // Same contract as the real store: a failed write changes nothing.
            if (failWrites) {
                throw new UncheckedIOException(new IOException("disk full"));
            }
            events.put(updated.getEventId(), updated);
        }
    }

    /** Stands in for the session; only the username matters here. */
    private static class FakeCurrentUser implements CurrentUserInterface {

        private String username = "";

        @Override
        public String getCurrentUsername() {
            return username;
        }

        @Override
        public void setCurrentUser(User user) {
            this.username = user.getUsername();
        }

        @Override
        public void clearCurrentUser() {
            this.username = "";
        }
    }

    /** Captures whatever the interactor asks the presenter to show. */
    private static class RecordingPresenter implements CancelEventOutputBoundary {

        private CancelEventOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(CancelEventOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new FakeCancelEventDataAccess();
        currentUser = new FakeCurrentUser();
        presenter = new RecordingPresenter();
        interactor = new CancelEventInteractor(dataAccess, presenter, currentUser);

        dataAccess.coaches.put("hawks", "coach1");
        game = new Event("game-1", "hawks", Event.EventType.GAME, WHEN, "Civic Arena", "Raptors");
        dataAccess.events.put("game-1", game);
        currentUser.username = "coach1";
    }

    @Test
    void coachCancelsEventAndLiveInstanceIsNotMutated() {
        interactor.execute(new CancelEventInputData("game-1"));

        assertNull(presenter.error);
        assertEquals("game-1", presenter.success.getEventId());
        final Event stored = dataAccess.events.get("game-1");
        assertNotSame(game, stored);
        assertTrue(stored.isCancelled());
        // Everything else carries over untouched.
        assertEquals(WHEN, stored.getDateTime());
        assertEquals("Civic Arena", stored.getLocation());
        assertEquals("Raptors", stored.getOpponent());
        assertEquals(Event.EventType.GAME, stored.getType());
        assertFalse(game.isCancelled());
    }

    @Test
    void failsWhenNotTheTeamsCoach() {
        currentUser.username = "coach2";

        interactor.execute(new CancelEventInputData("game-1"));

        assertNull(presenter.success);
        assertEquals("Only the team's coach can cancel this event.", presenter.error);
        assertSame(game, dataAccess.events.get("game-1"));
        assertFalse(game.isCancelled());
    }

    @Test
    void failsWhenAlreadyCancelled() {
        game.cancel();

        interactor.execute(new CancelEventInputData("game-1"));

        assertNull(presenter.success);
        assertEquals("This event is already cancelled.", presenter.error);
        assertSame(game, dataAccess.events.get("game-1"));
    }

    @Test
    void failsWhenEventNotFound() {
        interactor.execute(new CancelEventInputData("nope"));

        assertNull(presenter.success);
        assertEquals("That event no longer exists.", presenter.error);
    }

    @Test
    void failsWhenNobodyIsSignedIn() {
        currentUser.clearCurrentUser();

        interactor.execute(new CancelEventInputData("game-1"));

        assertNull(presenter.success);
        assertEquals("Sign in to cancel events.", presenter.error);
        assertFalse(game.isCancelled());
    }

    @Test
    void writeFailureLeavesEventUncancelled() {
        dataAccess.failWrites = true;

        interactor.execute(new CancelEventInputData("game-1"));

        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Could not save the cancellation:"));
        assertSame(game, dataAccess.events.get("game-1"));
        assertFalse(game.isCancelled());
    }
}
