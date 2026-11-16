package use_case.assign_coach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Role;
import entity.Team;
import entity.User;

/** Checks who may assign a coach and which users can become one. */
class AssignCoachInteractorTest {
    private FakeStore store;
    private RecordingPresenter presenter;
    private AssignCoachInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        store.users.put("admin1", new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.users.put("coach1", new User("coach1", "pw", "Coach One", Role.COACH));
        store.users.put("coach2", new User("coach2", "pw", "Coach Two", Role.COACH));
        store.users.put("player1", new User("player1", "pw", "Pat Player", Role.PLAYER));
        store.teams.put("t1", new Team("t1", "Raptors", "Basketball", "coach1"));
        presenter = new RecordingPresenter();
        interactor = new AssignCoachInteractor(store, presenter);
    }

    @Test
    void adminReplacesTheCoachAndTheTeamIsSaved() {
        interactor.execute(new AssignCoachInputData("admin1", " t1 ", " coach2 "));

        assertNull(presenter.error);
        assertEquals("coach2", store.teams.get("t1").getCoachUsername());
        assertEquals(1, store.saveCount);
        assertEquals("t1", presenter.success.getTeamId());
        assertEquals("coach2", presenter.success.getCoachUsername());
    }

    @Test
    void nonAdminIsRejected() {
        interactor.execute(new AssignCoachInputData("coach1", "t1", "coach2"));

        assertEquals("Only an admin can assign coaches.", presenter.error);
        assertEquals("coach1", store.teams.get("t1").getCoachUsername());
    }

    @Test
    void unknownTeamIsRejected() {
        interactor.execute(new AssignCoachInputData("admin1", "missing", "coach2"));

        assertEquals("That team does not exist.", presenter.error);
        assertEquals(0, store.saveCount);
    }

    @Test
    void userWhoIsNotACoachIsRejected() {
        interactor.execute(new AssignCoachInputData("admin1", "t1", "player1"));

        assertEquals("That user is not a coach.", presenter.error);
        assertEquals("coach1", store.teams.get("t1").getCoachUsername());
    }

    @Test
    void unknownCoachIsRejected() {
        interactor.execute(new AssignCoachInputData("admin1", "t1", "ghost"));

        assertEquals("That user is not a coach.", presenter.error);
        assertEquals(0, store.saveCount);
    }

    @Test
    void blankInputIsRejected() {
        interactor.execute(new AssignCoachInputData("admin1", "t1", " "));

        assertEquals("Enter a team ID and a coach username.", presenter.error);
        assertEquals(0, store.saveCount);
    }

    @Test
    void storageFailureIsReportedInsteadOfSuccess() {
        store.failOnSave = true;

        interactor.execute(new AssignCoachInputData("admin1", "t1", "coach2"));

        assertNull(presenter.success);
        assertEquals("Could not save the team: disk full", presenter.error);
    }

    private static final class FakeStore implements AssignCoachDataAccessInterface {
        private final Map<String, User> users = new HashMap<>();
        private final Map<String, Team> teams = new HashMap<>();
        private int saveCount;
        private boolean failOnSave;

        @Override
        public User get(String username) {
            return users.get(username);
        }

        @Override
        public Team getTeam(String teamId) {
            return teams.get(teamId);
        }

        @Override
        public void saveTeam(Team team) {
            if (failOnSave) {
                throw new UncheckedIOException("disk full", new IOException("disk full"));
            }
            teams.put(team.getTeamId(), team);
            saveCount++;
        }
    }

    private static final class RecordingPresenter implements AssignCoachOutputBoundary {
        private AssignCoachOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(AssignCoachOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
