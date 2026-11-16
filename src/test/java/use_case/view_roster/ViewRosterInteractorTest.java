package use_case.view_roster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Role;
import entity.Team;
import entity.User;
import use_case.view_roster.ViewRosterOutputData.TeamRoster;

/** Checks which teams each role can see. */
class ViewRosterInteractorTest {
    private FakeStore store;
    private RecordingPresenter presenter;
    private ViewRosterInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        store.users.put("admin1", new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.users.put("coach1", new User("coach1", "pw", "Coach One", Role.COACH));
        store.users.put("coach3", new User("coach3", "pw", "Coach Three", Role.COACH));
        store.users.put("player1", new User("player1", "pw", "Pat Player", Role.PLAYER));
        final Team raptors = new Team("t1", "Raptors", "Basketball", "coach1");
        raptors.addPlayer("player1");
        store.teams.add(raptors);
        store.teams.add(new Team("t2", "Lions", "Soccer", "coach2"));
        presenter = new RecordingPresenter();
        interactor = new ViewRosterInteractor(store, presenter);
    }

    @Test
    void adminSeesEveryTeamWithItsRoster() {
        interactor.execute(new ViewRosterInputData("admin1"));

        assertNull(presenter.error);
        final List<TeamRoster> teams = presenter.success.getTeams();
        assertEquals(2, teams.size());
        assertEquals("t1", teams.get(0).getTeamId());
        assertEquals("Raptors", teams.get(0).getName());
        assertEquals("Basketball", teams.get(0).getSport());
        assertEquals("coach1", teams.get(0).getCoachUsername());
        assertEquals(List.of("player1"), teams.get(0).getPlayerUsernames());
        assertEquals("t2", teams.get(1).getTeamId());
    }

    @Test
    void coachSeesOnlyTheTeamsTheyCoach() {
        interactor.execute(new ViewRosterInputData("coach1"));

        assertNull(presenter.error);
        assertEquals(1, presenter.success.getTeams().size());
        assertEquals("t1", presenter.success.getTeams().get(0).getTeamId());
    }

    @Test
    void coachWithNoTeamsSeesAnEmptyList() {
        interactor.execute(new ViewRosterInputData("coach3"));

        assertNull(presenter.error);
        assertTrue(presenter.success.getTeams().isEmpty());
    }

    @Test
    void playerIsRejected() {
        interactor.execute(new ViewRosterInputData("player1"));

        assertNull(presenter.success);
        assertEquals("Only admins and coaches can view rosters.", presenter.error);
    }

    @Test
    void unknownUserIsRejected() {
        interactor.execute(new ViewRosterInputData("ghost"));

        assertNull(presenter.success);
        assertEquals("Only admins and coaches can view rosters.", presenter.error);
    }

    private static final class FakeStore implements ViewRosterDataAccessInterface {
        private final Map<String, User> users = new HashMap<>();
        private final List<Team> teams = new ArrayList<>();

        @Override
        public User get(String username) {
            return users.get(username);
        }

        @Override
        public List<Team> getAllTeams() {
            return new ArrayList<>(teams);
        }
    }

    private static final class RecordingPresenter implements ViewRosterOutputBoundary {
        private ViewRosterOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ViewRosterOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
