package use_case.browse_events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
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
 * Tests the Browse Events use case: listing a user's events, every filter
 * combination, the no-team case, and every way it can fail.
 */
class BrowseEventsInteractorTest {

    private FakeBrowseEventsDataAccess dataAccess;
    private FakeCurrentUser currentUser;
    private RecordingPresenter presenter;
    private BrowseEventsInteractor interactor;

    /** Teams and events held in memory, scoped to what this use case reads. */
    private static class FakeBrowseEventsDataAccess implements BrowseEventsDataAccessInterface {

        private final Map<String, List<String>> teamsByMember = new HashMap<>();
        private final Map<String, String> teamNames = new HashMap<>();
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
        public String getTeamName(String teamId) {
            return teamNames.get(teamId);
        }

        @Override
        public List<Event> getEventsForTeams(Collection<String> teamIds) {
            final List<Event> matches = new ArrayList<>();
            for (Event event : events) {
                if (teamIds.contains(event.getTeamId())) {
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
    private static class RecordingPresenter implements BrowseEventsOutputBoundary {

        private BrowseEventsOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(BrowseEventsOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new FakeBrowseEventsDataAccess();
        currentUser = new FakeCurrentUser();
        presenter = new RecordingPresenter();
        interactor = new BrowseEventsInteractor(dataAccess, presenter, currentUser);

        // coach1 coaches two teams; a third team belongs to someone else.
        dataAccess.teamsByMember.put("coach1", List.of("hawks", "owls"));
        dataAccess.teamNames.put("hawks", "Hawks");
        dataAccess.teamNames.put("owls", "Owls");
        dataAccess.teamNames.put("wolves", "Wolves");

        dataAccess.events.add(practice("h-prac", "hawks", LocalDateTime.of(2026, 11, 24, 17, 0)));
        dataAccess.events.add(game("h-game", "hawks", LocalDateTime.of(2026, 11, 10, 18, 0)));
        dataAccess.events.add(practice("o-prac", "owls", LocalDateTime.of(2026, 12, 3, 18, 0)));
        dataAccess.events.add(game("w-game", "wolves", LocalDateTime.of(2026, 11, 18, 18, 0)));

        final Event cancelled = practice("h-cancelled", "hawks",
                LocalDateTime.of(2026, 11, 19, 18, 0));
        cancelled.cancel();
        dataAccess.events.add(cancelled);

        currentUser.username = "coach1";
    }

    private static Event practice(String id, String teamId, LocalDateTime when) {
        return new Event(id, teamId, Event.EventType.PRACTICE, when, "Gym", null);
    }

    private static Event game(String id, String teamId, LocalDateTime when) {
        return new Event(id, teamId, Event.EventType.GAME, when, "Arena", "Rivals");
    }

    private List<String> ids() {
        final List<String> ids = new ArrayList<>();
        for (BrowseEventsOutputData.EventRow row : presenter.success.getEvents()) {
            ids.add(row.getEventId());
        }
        return ids;
    }

    @Test
    void listsOwnTeamsEventsEarliestFirstWithCancelledFlagged() {
        interactor.execute(new BrowseEventsInputData(null, null));

        assertNull(presenter.error);
        assertTrue(presenter.success.isOnAnyTeam());
        assertFalse(presenter.success.isFiltered());
        assertEquals(List.of("h-game", "h-cancelled", "h-prac", "o-prac"), ids());

        final BrowseEventsOutputData.EventRow cancelled = presenter.success.getEvents().get(1);
        assertTrue(cancelled.isCancelled());
        assertEquals("Hawks", cancelled.getTeamName());
        assertEquals("Owls", presenter.success.getEvents().get(3).getTeamName());

        final List<BrowseEventsOutputData.TeamOption> teams = presenter.success.getTeams();
        assertEquals(2, teams.size());
        assertEquals("hawks", teams.get(0).getTeamId());
        assertEquals("Owls", teams.get(1).getName());
    }

    @Test
    void filtersByType() {
        interactor.execute(new BrowseEventsInputData("GAME", null));

        assertTrue(presenter.success.isFiltered());
        assertEquals(List.of("h-game"), ids());
    }

    @Test
    void filtersByTeam() {
        interactor.execute(new BrowseEventsInputData(null, "owls"));

        assertTrue(presenter.success.isFiltered());
        assertEquals(List.of("o-prac"), ids());
        // The team list still offers every team, so the filter can be changed back.
        assertEquals(2, presenter.success.getTeams().size());
    }

    @Test
    void filtersByTypeAndTeamTogether() {
        interactor.execute(new BrowseEventsInputData("PRACTICE", "hawks"));

        assertEquals(List.of("h-cancelled", "h-prac"), ids());
    }

    @Test
    void filterCombinationWithNoMatchesIsEmptyAndMarkedFiltered() {
        interactor.execute(new BrowseEventsInputData("GAME", "owls"));

        assertNull(presenter.error);
        assertTrue(presenter.success.getEvents().isEmpty());
        assertTrue(presenter.success.isFiltered());
        assertTrue(presenter.success.isOnAnyTeam());
    }

    @Test
    void teamWithNoEventsIsEmptyButNotFiltered() {
        dataAccess.events.clear();

        interactor.execute(new BrowseEventsInputData(null, null));

        assertTrue(presenter.success.getEvents().isEmpty());
        assertFalse(presenter.success.isFiltered());
        assertTrue(presenter.success.isOnAnyTeam());
    }

    @Test
    void userOnNoTeamGetsEmptyListFlaggedAsNoTeam() {
        currentUser.username = "parent1";

        interactor.execute(new BrowseEventsInputData(null, null));

        assertNull(presenter.error);
        assertFalse(presenter.success.isOnAnyTeam());
        assertTrue(presenter.success.getEvents().isEmpty());
        assertTrue(presenter.success.getTeams().isEmpty());
    }

    @Test
    void failsWhenFilteringByATeamTheUserIsNotOn() {
        interactor.execute(new BrowseEventsInputData(null, "wolves"));

        assertNull(presenter.success);
        assertEquals("You're not on that team.", presenter.error);
    }

    @Test
    void failsOnUnknownType() {
        interactor.execute(new BrowseEventsInputData("TOURNAMENT", null));

        assertNull(presenter.success);
        assertEquals("Unknown event type: TOURNAMENT", presenter.error);
    }

    @Test
    void failsWhenNobodyIsSignedIn() {
        currentUser.clearCurrentUser();

        interactor.execute(new BrowseEventsInputData(null, null));

        assertNull(presenter.success);
        assertEquals("Sign in to see your events.", presenter.error);
    }

    @Test
    void reportsDataAccessFailure() {
        dataAccess.failReads = true;

        interactor.execute(new BrowseEventsInputData(null, null));

        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Could not read your events:"));
    }
}
