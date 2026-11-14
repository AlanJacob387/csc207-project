package use_case.view_schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Event;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Tests the View Schedule use case: loading a user's events for a range, the
 * no-team case, and every way it can fail.
 */
class ViewScheduleInteractorTest {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    private FakeScheduleDataAccess dataAccess;
    private FakeCurrentUser currentUser;
    private RecordingPresenter presenter;
    private ViewScheduleInteractor interactor;

    /** Teams and events held in memory, scoped to what this use case reads. */
    private static class FakeScheduleDataAccess implements ViewScheduleDataAccessInterface {

        private final Map<String, List<String>> teamsByMember = new HashMap<>();
        private final List<Event> events = new ArrayList<>();
        private boolean failReads;

        @Override
        public List<String> getTeamIdsForMember(String username) {
            if (failReads) {
                throw new UncheckedIOException(new IOException("disk unplugged"));
            }
            return new ArrayList<>(teamsByMember.getOrDefault(username, new ArrayList<>()));
        }

        @Override
        public List<Event> getEventsForTeams(Collection<String> teamIds,
                                             LocalDate from, LocalDate to) {
            final List<Event> matches = new ArrayList<>();
            for (Event event : events) {
                final LocalDate date = event.getDateTime().toLocalDate();
                if (teamIds.contains(event.getTeamId())
                        && !date.isBefore(from) && !date.isAfter(to)) {
                    matches.add(event);
                }
            }
            return matches;
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
    private static class RecordingPresenter implements ViewScheduleOutputBoundary {

        private ViewScheduleOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ViewScheduleOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new FakeScheduleDataAccess();
        currentUser = new FakeCurrentUser();
        presenter = new RecordingPresenter();
        interactor = new ViewScheduleInteractor(dataAccess, presenter, currentUser);

        dataAccess.teamsByMember.put("player1", List.of("hawks"));
        currentUser.username = "player1";
    }

    private static Event event(String id, String teamId, LocalDateTime when) {
        return new Event(id, teamId, Event.EventType.PRACTICE, when, "Gym", null);
    }

    @Test
    void returnsOnlyOwnTeamEventsInRangeEarliestFirst() {
        dataAccess.events.add(event("late", "hawks", LocalDateTime.of(2026, 11, 20, 18, 0)));
        dataAccess.events.add(event("early", "hawks", LocalDateTime.of(2026, 11, 3, 18, 0)));
        dataAccess.events.add(event("other-team", "eagles", LocalDateTime.of(2026, 11, 5, 18, 0)));
        dataAccess.events.add(event("next-month", "hawks", LocalDateTime.of(2026, 12, 2, 18, 0)));

        final Event cancelled = new Event("game", "hawks", Event.EventType.GAME,
                LocalDateTime.of(2026, 11, 10, 14, 0), "Field", "Rivals");
        cancelled.cancel();
        dataAccess.events.add(cancelled);

        interactor.execute(new ViewScheduleInputData(NOV_1, NOV_30));

        assertNull(presenter.error);
        assertTrue(presenter.success.isOnAnyTeam());
        final List<ViewScheduleOutputData.EventRow> rows = presenter.success.getEvents();
        assertEquals(3, rows.size());
        assertEquals("early", rows.get(0).getEventId());
        assertEquals("game", rows.get(1).getEventId());
        assertEquals("late", rows.get(2).getEventId());

        final ViewScheduleOutputData.EventRow game = rows.get(1);
        assertEquals("GAME", game.getType());
        assertEquals("Rivals", game.getOpponent());
        assertTrue(game.isCancelled());
    }

    @Test
    void userOnNoTeamGetsEmptyScheduleFlaggedAsNoTeam() {
        currentUser.username = "parent1";

        interactor.execute(new ViewScheduleInputData(NOV_1, NOV_30));

        assertNull(presenter.error);
        assertFalse(presenter.success.isOnAnyTeam());
        assertTrue(presenter.success.getEvents().isEmpty());
    }

    @Test
    void teamWithNoEventsInRangeIsStillOnATeam() {
        interactor.execute(new ViewScheduleInputData(NOV_1, NOV_30));

        assertTrue(presenter.success.isOnAnyTeam());
        assertTrue(presenter.success.getEvents().isEmpty());
    }

    @Test
    void failsWhenNobodyIsSignedIn() {
        currentUser.clearCurrentUser();

        interactor.execute(new ViewScheduleInputData(NOV_1, NOV_30));

        assertNull(presenter.success);
        assertEquals("Sign in to see the schedule.", presenter.error);
    }

    @Test
    void failsWhenRangeIsReversed() {
        interactor.execute(new ViewScheduleInputData(NOV_30, NOV_1));

        assertNull(presenter.success);
        assertEquals("The start of the range is after the end.", presenter.error);
    }

    @Test
    void failsWhenRangeIsMissing() {
        interactor.execute(new ViewScheduleInputData(null, NOV_30));

        assertNull(presenter.success);
        assertEquals("No date range was given.", presenter.error);
    }

    @Test
    void reportsDataAccessFailure() {
        dataAccess.failReads = true;

        interactor.execute(new ViewScheduleInputData(NOV_1, NOV_30));

        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Could not read the schedule:"));
    }
}
