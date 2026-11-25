package use_case.create_event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Event;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Tests the Create Event use case: creating practices and games, every rule
 * the interactor checks, who may create events, and what happens when saving
 * fails.
 */
class CreateEventInteractorTest {

    private static final LocalDateTime FUTURE = LocalDateTime.now().plusDays(7).withNano(0);
    private static final LocalDateTime PAST = LocalDateTime.of(2020, 1, 15, 18, 0);

    private FakeCreateEventDataAccess dataAccess;
    private FakeCurrentUser currentUser;
    private RecordingPresenter presenter;
    private CreateEventInteractor interactor;

    /** Teams and saved events held in memory, scoped to what this use case needs. */
    private static class FakeCreateEventDataAccess implements CreateEventDataAccessInterface {

        // team id -> coach username
        private final Map<String, String> teams = new HashMap<>();
        private final List<Event> saved = new ArrayList<>();
        private boolean failWrites;
        private boolean failAuthorityRead;

        @Override
        public void saveEvent(Event event) {
            // Behaves like a correct store: a failed write keeps nothing.
            if (failWrites) {
                throw new UncheckedIOException(new IOException("disk full"));
            }
            saved.add(event);
        }

        @Override
        public boolean teamExists(String teamId) {
            return teams.containsKey(teamId);
        }

        @Override
        public String getCoachUsername(String teamId) {
            return teams.get(teamId);
        }

        @Override
        public boolean canManageEvent(String username, String teamId) {
            if (failAuthorityRead) {
                throw new IllegalStateException("Account authority is unavailable");
            }
            return username != null && username.equals(teams.get(teamId));
        }
    }

    /** Stands in for the session; only the username matters here. */
    private static class FakeCurrentUser implements CurrentUserInterface {
        private entity.Role currentRole;

        @Override
        public entity.Role getCurrentRole() {
            return currentRole;
        }


        private String username = "";

        @Override
        public String getCurrentUsername() {
            return username;
        }

        @Override
        public void setCurrentUser(User user) {
            currentRole = user.getRole();
            this.username = user.getUsername();
        }

        @Override
        public void clearCurrentUser() {
            this.username = "";
        }
    }

    /** Captures whatever the interactor asks the presenter to show. */
    private static class RecordingPresenter implements CreateEventOutputBoundary {

        private CreateEventOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(CreateEventOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new FakeCreateEventDataAccess();
        currentUser = new FakeCurrentUser();
        presenter = new RecordingPresenter();
        interactor = new CreateEventInteractor(dataAccess, presenter, currentUser);

        dataAccess.teams.put("hawks", "coach1");
        dataAccess.teams.put("owls", "coach2");
        currentUser.username = "coach1";
    }

    private void assertFailedWithNothingSaved(String expectedError) {
        assertNull(presenter.success);
        assertEquals(expectedError, presenter.error);
        assertTrue(dataAccess.saved.isEmpty());
    }

    @Test
    void createsGameAndSavesEveryField() {
        interactor.execute(new CreateEventInputData("hawks", "GAME", FUTURE, "North Court", "Raptors"));

        assertNull(presenter.error);
        assertEquals(1, dataAccess.saved.size());
        final Event saved = dataAccess.saved.get(0);
        assertNotNull(saved.getEventId());
        assertEquals("hawks", saved.getTeamId());
        assertEquals(Event.EventType.GAME, saved.getType());
        assertEquals(FUTURE, saved.getDateTime());
        assertEquals("North Court", saved.getLocation());
        assertEquals("Raptors", saved.getOpponent());
        assertEquals(false, saved.isCancelled());

        assertEquals(saved.getEventId(), presenter.success.getEventId());
        assertEquals("Event created.", presenter.success.getMessage());
    }

    @Test
    void createsPracticeWithNoOpponent() {
        interactor.execute(new CreateEventInputData("hawks", "PRACTICE", FUTURE, "Main Gym", null));

        assertNull(presenter.error);
        final Event saved = dataAccess.saved.get(0);
        assertEquals(Event.EventType.PRACTICE, saved.getType());
        assertEquals("Main Gym", saved.getLocation());
        assertNull(saved.getOpponent());
    }

    /**
     * The New Event form sends the opponent field's trimmed text, so a practice
     * arrives with "" rather than null. edit_event stores practices with a null
     * opponent, and create_event should agree.
     */
    @Test
    void practiceOpponentIsStoredAsNullEvenWhenFormSendsBlank() {
        interactor.execute(new CreateEventInputData("hawks", "PRACTICE", FUTURE, "Main Gym", ""));

        assertNull(presenter.error);
        assertNull(dataAccess.saved.get(0).getOpponent());
    }

    @Test
    void failsWhenNobodyIsSignedIn() {
        currentUser.clearCurrentUser();

        interactor.execute(new CreateEventInputData("hawks", "GAME", FUTURE, "Arena", "Raptors"));

        assertFailedWithNothingSaved("Sign in to create events.");
    }

    @Test
    void failsWhenNotTheTeamsCoach() {
        // coach1 is a coach, just not of this team.
        interactor.execute(new CreateEventInputData("owls", "GAME", FUTURE, "Arena", "Raptors"));

        assertFailedWithNothingSaved("Only the team's coach can create events for this team.");
    }

    @Test
    void failsWhenTeamDoesNotExist() {
        interactor.execute(new CreateEventInputData("no-such-team", "GAME", FUTURE, "Gym", null));

        assertFailedWithNothingSaved("Team does not exist: no-such-team");
    }

    @Test
    void failsWhenDateTimeMissing() {
        interactor.execute(new CreateEventInputData("hawks", "PRACTICE", null, "Gym", null));

        assertFailedWithNothingSaved("Event date/time must be in the future.");
    }

    @Test
    void failsWhenDateTimeInThePast() {
        interactor.execute(new CreateEventInputData("hawks", "PRACTICE", PAST, "Gym", null));

        assertFailedWithNothingSaved("Event date/time must be in the future.");
    }

    @Test
    void failsWhenLocationBlank() {
        interactor.execute(new CreateEventInputData("hawks", "PRACTICE", FUTURE, "   ", null));

        assertFailedWithNothingSaved("Location is required.");
    }

    @Test
    void failsOnUnknownEventType() {
        interactor.execute(new CreateEventInputData("hawks", "TOURNAMENT", FUTURE, "Gym", null));

        assertFailedWithNothingSaved("Unknown event type: TOURNAMENT");
    }

    /** A missing type is part of the type check, so it should fail cleanly too. */
    @Test
    void failsCleanlyWhenEventTypeMissing() {
        assertDoesNotThrow(() ->
                interactor.execute(new CreateEventInputData("hawks", null, FUTURE, "Gym", null)));

        assertNull(presenter.success);
        assertNotNull(presenter.error);
        assertTrue(dataAccess.saved.isEmpty());
    }

    /** A failed save must reach the user as a message, not escape as an exception. */
    @Test
    void writeFailureIsReportedThroughFailView() {
        dataAccess.failWrites = true;

        assertDoesNotThrow(() ->
                interactor.execute(new CreateEventInputData("hawks", "GAME", FUTURE, "Arena", "Raptors")));

        assertNull(presenter.success);
        assertNotNull(presenter.error);
        assertTrue(dataAccess.saved.isEmpty());
    }

    @Test
    void anAuthorityReadFailureIsReportedBeforeAnyEventIsSaved() {
        dataAccess.failAuthorityRead = true;

        interactor.execute(new CreateEventInputData("hawks", "GAME", FUTURE, "Arena", "Rivals"));

        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Could not read the team:"));
        assertTrue(dataAccess.saved.isEmpty());
    }
}
