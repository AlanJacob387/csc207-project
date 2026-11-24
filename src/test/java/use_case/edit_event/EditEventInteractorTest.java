package use_case.edit_event;

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
 * Tests the Edit Event use case: a successful edit, the past-event location
 * fix, and every way it can fail, including a failed save.
 */
class EditEventInteractorTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(10).withNano(0);
    private static final LocalDateTime PAST = LocalDateTime.of(2020, 1, 15, 18, 0);

    private FakeEditEventDataAccess dataAccess;
    private FakeCurrentUser currentUser;
    private RecordingPresenter presenter;
    private EditEventInteractor interactor;
    private Event game;

    /** Events and coaches held in memory, scoped to what this use case needs. */
    private static class FakeEditEventDataAccess implements EditEventDataAccessInterface {

        private final Map<String, Event> events = new HashMap<>();
        private final Map<String, String> coaches = new HashMap<>();
        private boolean failWrites;
        private boolean failAuthorityRead;

        @Override
        public Event getEvent(String eventId) {
            return events.get(eventId);
        }

        @Override
        public String getCoachUsername(String teamId) {
            return coaches.get(teamId);
        }

        @Override
        public boolean canManageEvent(String username, String teamId) {
            if (failAuthorityRead) {
                throw new IllegalStateException("Account authority is unavailable");
            }
            return username != null && username.equals(coaches.get(teamId));
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
    private static class RecordingPresenter implements EditEventOutputBoundary {

        private EditEventOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(EditEventOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new FakeEditEventDataAccess();
        currentUser = new FakeCurrentUser();
        presenter = new RecordingPresenter();
        interactor = new EditEventInteractor(dataAccess, presenter, currentUser);

        dataAccess.coaches.put("hawks", "coach1");
        game = new Event("game-1", "hawks", Event.EventType.GAME, FUTURE, "North Field", "Rivals");
        dataAccess.events.put("game-1", game);
        currentUser.username = "coach1";
    }

    private void assertGameUnchanged() {
        assertSame(game, dataAccess.events.get("game-1"));
        assertEquals(FUTURE, game.getDateTime());
        assertEquals("North Field", game.getLocation());
        assertEquals("Rivals", game.getOpponent());
        assertFalse(game.isCancelled());
    }

    @Test
    void coachEditsEventAndLiveInstanceIsNotMutated() {
        final LocalDateTime later = FUTURE.plusDays(1);

        interactor.execute(new EditEventInputData("game-1", later, "  South Field ", " Wolves "));

        assertNull(presenter.error);
        assertEquals("game-1", presenter.success.getEventId());
        final Event stored = dataAccess.events.get("game-1");
        assertNotSame(game, stored);
        assertEquals(later, stored.getDateTime());
        assertEquals("South Field", stored.getLocation());
        assertEquals("Wolves", stored.getOpponent());
        assertEquals(Event.EventType.GAME, stored.getType());
        assertEquals("hawks", stored.getTeamId());
        // The object handed out by getEvent was only read, never changed.
        assertEquals("North Field", game.getLocation());
    }

    @Test
    void locationOfPastEventCanBeFixedWhenDateUnchanged() {
        final Event old = new Event("old", "hawks", Event.EventType.PRACTICE, PAST, "Gym A", null);
        dataAccess.events.put("old", old);

        interactor.execute(new EditEventInputData("old", PAST, "Gym B", ""));

        assertNull(presenter.error);
        assertEquals("Gym B", dataAccess.events.get("old").getLocation());
    }

    @Test
    void practiceOpponentIsStoredAsNull() {
        dataAccess.events.put("prac", new Event("prac", "hawks", Event.EventType.PRACTICE,
                FUTURE, "Gym", null));

        interactor.execute(new EditEventInputData("prac", FUTURE, "Gym", "Somebody"));

        assertNull(presenter.error);
        assertNull(dataAccess.events.get("prac").getOpponent());
    }

    @Test
    void failsWhenNotTheTeamsCoach() {
        currentUser.username = "player1";

        interactor.execute(new EditEventInputData("game-1", FUTURE, "Elsewhere", "Rivals"));

        assertNull(presenter.success);
        assertEquals("Only the team's coach can edit this event.", presenter.error);
        assertGameUnchanged();
    }

    @Test
    void failsWhenNewDateTimeIsInThePast() {
        interactor.execute(new EditEventInputData("game-1", PAST, "North Field", "Rivals"));

        assertNull(presenter.success);
        assertEquals("Event date/time must be in the future.", presenter.error);
        assertGameUnchanged();
    }

    @Test
    void failsWhenDateTimeMissing() {
        interactor.execute(new EditEventInputData("game-1", null, "North Field", "Rivals"));

        assertNull(presenter.success);
        assertEquals("Enter a date and time.", presenter.error);
    }

    @Test
    void failsWhenLocationBlank() {
        interactor.execute(new EditEventInputData("game-1", FUTURE, "  ", "Rivals"));

        assertNull(presenter.success);
        assertEquals("Location is required.", presenter.error);
        assertGameUnchanged();
    }

    @Test
    void failsWhenEventIsCancelled() {
        game.cancel();

        interactor.execute(new EditEventInputData("game-1", FUTURE, "Elsewhere", "Rivals"));

        assertNull(presenter.success);
        assertEquals("Cancelled events can't be edited.", presenter.error);
        assertEquals("North Field", game.getLocation());
    }

    @Test
    void failsWhenEventNotFound() {
        interactor.execute(new EditEventInputData("nope", FUTURE, "Gym", null));

        assertNull(presenter.success);
        assertEquals("That event no longer exists.", presenter.error);
    }

    @Test
    void failsWhenNobodyIsSignedIn() {
        currentUser.clearCurrentUser();

        interactor.execute(new EditEventInputData("game-1", FUTURE, "Elsewhere", "Rivals"));

        assertNull(presenter.success);
        assertEquals("Sign in to edit events.", presenter.error);
        assertGameUnchanged();
    }

    @Test
    void writeFailureLeavesEventUnchanged() {
        dataAccess.failWrites = true;

        interactor.execute(new EditEventInputData("game-1", FUTURE.plusDays(3),
                "South Field", "Wolves"));

        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Could not save the change:"));
        assertGameUnchanged();
    }

    @Test
    void anAuthorityReadFailureIsReportedWithoutChangingTheEvent() {
        dataAccess.failAuthorityRead = true;

        interactor.execute(new EditEventInputData("game-1", FUTURE, "Elsewhere", "Rivals"));

        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Could not read the event:"));
        assertGameUnchanged();
    }
}
