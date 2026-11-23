package use_case.view_event_details;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Tests the View Event Details use case: showing an event to its coach and to
 * a player, and every way it can fail.
 */
class ViewEventDetailsInteractorTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 20, 18, 30);

    private FakeEventDetailsDataAccess dataAccess;
    private FakeCurrentUser currentUser;
    private RecordingPresenter presenter;
    private ViewEventDetailsInteractor interactor;

    /** Events and teams held in memory, scoped to what this use case reads. */
    private static class FakeEventDetailsDataAccess implements ViewEventDetailsDataAccessInterface {

        private final Map<String, Event> events = new HashMap<>();
        private final Map<String, String> teamNames = new HashMap<>();
        private final Map<String, String> coaches = new HashMap<>();
        private boolean failReads;
        private boolean failAuthorityRead;

        @Override
        public Event getEvent(String eventId) {
            if (failReads) {
                throw new UncheckedIOException(new IOException("disk unplugged"));
            }
            return events.get(eventId);
        }

        @Override
        public String getTeamName(String teamId) {
            return teamNames.get(teamId);
        }

        @Override
        public String getCoachUsername(String teamId) {
            return coaches.get(teamId);
        }

        @Override
        public boolean canViewEvent(String username, String teamId) {
            return "hawks".equals(teamId) && ("coach1".equals(username) || "player1".equals(username));
        }

        @Override
        public boolean canManageEvent(String username, String teamId) {
            if (failAuthorityRead) {
                throw new IllegalStateException("Account authority is unavailable");
            }
            return username != null && username.equals(coaches.get(teamId));
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
    private static class RecordingPresenter implements ViewEventDetailsOutputBoundary {

        private ViewEventDetailsOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ViewEventDetailsOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new FakeEventDetailsDataAccess();
        currentUser = new FakeCurrentUser();
        presenter = new RecordingPresenter();
        interactor = new ViewEventDetailsInteractor(dataAccess, presenter, currentUser);

        dataAccess.teamNames.put("hawks", "Hawks");
        dataAccess.coaches.put("hawks", "coach1");
        dataAccess.events.put("game-1", new Event("game-1", "hawks", Event.EventType.GAME,
                WHEN, "North Field", "Rivals"));
    }

    @Test
    void coachSeesFullDetailsAndMayManage() {
        currentUser.username = "coach1";

        interactor.execute(new ViewEventDetailsInputData("game-1"));

        assertNull(presenter.error);
        final ViewEventDetailsOutputData out = presenter.success;
        assertEquals("game-1", out.getEventId());
        assertEquals("Hawks", out.getTeamName());
        assertEquals("GAME", out.getType());
        assertEquals(WHEN, out.getDateTime());
        assertEquals("North Field", out.getLocation());
        assertEquals("Rivals", out.getOpponent());
        assertFalse(out.isCancelled());
        assertTrue(out.isCanManage());
    }

    @Test
    void playerSeesDetailsButMayNotManage() {
        currentUser.username = "player1";

        interactor.execute(new ViewEventDetailsInputData("game-1"));

        assertNull(presenter.error);
        assertFalse(presenter.success.isCanManage());
    }

    @Test
    void cancelledEventIsReportedAsCancelled() {
        currentUser.username = "player1";
        dataAccess.events.get("game-1").cancel();

        interactor.execute(new ViewEventDetailsInputData("game-1"));

        assertTrue(presenter.success.isCancelled());
    }

    @Test
    void missingTeamFallsBackToTeamId() {
        currentUser.username = "player1";
        dataAccess.teamNames.clear();
        dataAccess.coaches.clear();

        interactor.execute(new ViewEventDetailsInputData("game-1"));

        assertEquals("hawks", presenter.success.getTeamName());
        assertFalse(presenter.success.isCanManage());
    }

    @Test
    void failsWhenEventNotFound() {
        currentUser.username = "coach1";

        interactor.execute(new ViewEventDetailsInputData("no-such-event"));

        assertNull(presenter.success);
        assertEquals("That event no longer exists.", presenter.error);
    }

    @Test
    void failsWhenNobodyIsSignedIn() {
        interactor.execute(new ViewEventDetailsInputData("game-1"));

        assertNull(presenter.success);
        assertEquals("Sign in to see event details.", presenter.error);
    }

    @Test
    void failsWhenNoEventIdGiven() {
        currentUser.username = "coach1";

        interactor.execute(new ViewEventDetailsInputData(" "));

        assertNull(presenter.success);
        assertEquals("No event was selected.", presenter.error);
    }

    @Test
    void reportsDataAccessFailure() {
        currentUser.username = "coach1";
        dataAccess.failReads = true;

        interactor.execute(new ViewEventDetailsInputData("game-1"));

        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Could not read the event:"));
    }

    @Test
    void unrelatedUserCannotReadAnEventByGuessingItsIdentifier() {
        currentUser.username = "unrelated-player";

        interactor.execute(new ViewEventDetailsInputData("game-1"));

        assertNull(presenter.success);
        assertEquals("You cannot view events for that team.", presenter.error);
    }

    @Test
    void anAuthorityReadFailureIsReportedInsteadOfShowingEditableDetails() {
        currentUser.username = "coach1";
        dataAccess.failAuthorityRead = true;

        interactor.execute(new ViewEventDetailsInputData("game-1"));

        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Could not read the event:"));
    }
}
