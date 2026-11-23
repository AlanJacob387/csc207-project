package use_case.create_team;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Role;
import entity.Team;
import entity.User;

/** Checks who may create a team and which details are accepted. */
class CreateTeamInteractorTest {
    private FakeStore store;
    private RecordingPresenter presenter;
    private CreateTeamInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        store.users.put("admin1", new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.users.put("coach1", new User("coach1", "pw", "Coach One", Role.COACH));
        presenter = new RecordingPresenter();
        interactor = new CreateTeamInteractor(store, presenter);
    }

    @Test
    void adminCreatesATeamWithTrimmedDetailsAndNoCoach() {
        interactor.execute(new CreateTeamInputData("admin1", " u12-raptors ", " Raptors ", " Basketball "));

        assertNull(presenter.error);
        final Team saved = store.teams.get("u12-raptors");
        assertEquals("Raptors", saved.getName());
        assertEquals("Basketball", saved.getSport());
        assertEquals("", saved.getCoachUsername());
        assertTrue(saved.getPlayerUsernames().isEmpty());
        assertEquals("u12-raptors", presenter.success.getTeamId());
        assertEquals("Raptors", presenter.success.getName());
    }

    @Test
    void nonAdminIsRejected() {
        interactor.execute(new CreateTeamInputData("coach1", "t1", "Raptors", "Basketball"));

        assertEquals("Only an admin can create teams.", presenter.error);
        assertTrue(store.teams.isEmpty());
    }

    @Test
    void unknownUserIsRejected() {
        interactor.execute(new CreateTeamInputData("ghost", "t1", "Raptors", "Basketball"));

        assertEquals("Only an admin can create teams.", presenter.error);
        assertTrue(store.teams.isEmpty());
    }

    @Test
    void blankDetailsAreRejected() {
        interactor.execute(new CreateTeamInputData("admin1", "t1", "  ", "Basketball"));

        assertEquals("Enter a team ID, name and sport.", presenter.error);
        assertTrue(store.teams.isEmpty());
    }

    @Test
    void duplicateTeamIdIsRejected() {
        store.teams.put("t1", new Team("t1", "Old Team", "Soccer", "coach1"));

        interactor.execute(new CreateTeamInputData("admin1", "t1", "Raptors", "Basketball"));

        assertEquals("A team with ID \"t1\" already exists.", presenter.error);
        assertEquals("Old Team", store.teams.get("t1").getName());
    }

    @Test
    void storageFailureIsReportedInsteadOfSuccess() {
        store.failOnSave = true;

        interactor.execute(new CreateTeamInputData("admin1", "t1", "Raptors", "Basketball"));

        assertNull(presenter.success);
        assertEquals("Could not save the team: disk full", presenter.error);
    }

    private static final class FakeStore implements CreateTeamDataAccessInterface {
        private final Map<String, User> users = new HashMap<>();
        private final Map<String, Team> teams = new HashMap<>();
        private boolean failOnSave;

        @Override
        public User get(String username) {
            return users.get(username);
        }

        @Override
        public boolean teamExists(String teamId) {
            return teams.containsKey(teamId);
        }

        @Override
        public void saveTeam(Team team) {
            if (failOnSave) {
                throw new UncheckedIOException("disk full", new IOException("disk full"));
            }
            teams.put(team.getTeamId(), team);
        }
    }

    private static final class RecordingPresenter implements CreateTeamOutputBoundary {
        private CreateTeamOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(CreateTeamOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
