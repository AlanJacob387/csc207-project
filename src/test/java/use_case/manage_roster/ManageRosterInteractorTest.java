package use_case.manage_roster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Role;
import entity.Team;
import entity.User;

/** Checks adding and removing players, and who may do it. */
class ManageRosterInteractorTest {
    private FakeStore store;
    private RecordingPresenter presenter;
    private ManageRosterInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        store.users.put("admin1", new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.users.put("coach1", new User("coach1", "pw", "Coach One", Role.COACH));
        store.users.put("player1", new User("player1", "pw", "Pat Player", Role.PLAYER));
        store.users.put("player2", new User("player2", "pw", "Max Player", Role.PLAYER));
        final Team team = new Team("t1", "Raptors", "Basketball", "coach1");
        team.addPlayer("player1");
        store.teams.put("t1", team);
        presenter = new RecordingPresenter();
        interactor = new ManageRosterInteractor(store, presenter);
    }

    @Test
    void adminAddsAPlayer() {
        interactor.execute(new ManageRosterInputData("admin1", " t1 ", " player2 ", true));

        assertNull(presenter.error);
        assertEquals(List.of("player1", "player2"), store.teams.get("t1").getPlayerUsernames());
        assertEquals(1, store.saveCount);
        assertEquals("player2", presenter.success.getPlayerUsername());
        assertTrue(presenter.success.isAdded());
    }

    @Test
    void adminRemovesAPlayer() {
        interactor.execute(new ManageRosterInputData("admin1", "t1", "player1", false));

        assertNull(presenter.error);
        assertTrue(store.teams.get("t1").getPlayerUsernames().isEmpty());
        assertEquals(1, store.saveCount);
        assertFalse(presenter.success.isAdded());
    }

    @Test
    void nonAdminIsRejected() {
        interactor.execute(new ManageRosterInputData("coach1", "t1", "player2", true));

        assertEquals("Only an admin can change a roster.", presenter.error);
        assertEquals(List.of("player1"), store.teams.get("t1").getPlayerUsernames());
    }

    @Test
    void unknownTeamIsRejected() {
        interactor.execute(new ManageRosterInputData("admin1", "missing", "player2", true));

        assertEquals("That team does not exist.", presenter.error);
        assertEquals(0, store.saveCount);
    }

    @Test
    void addingAUserWhoIsNotAPlayerIsRejected() {
        interactor.execute(new ManageRosterInputData("admin1", "t1", "coach1", true));

        assertEquals("That user is not a player.", presenter.error);
        assertEquals(List.of("player1"), store.teams.get("t1").getPlayerUsernames());
    }

    @Test
    void addingAPlayerTwiceIsRejected() {
        interactor.execute(new ManageRosterInputData("admin1", "t1", "player1", true));

        assertEquals("That player is already on the team.", presenter.error);
        assertEquals(0, store.saveCount);
    }

    @Test
    void removingAPlayerWhoIsNotOnTheTeamIsRejected() {
        interactor.execute(new ManageRosterInputData("admin1", "t1", "player2", false));

        assertEquals("That player is not on the team.", presenter.error);
        assertEquals(0, store.saveCount);
    }

    @Test
    void blankInputIsRejected() {
        interactor.execute(new ManageRosterInputData("admin1", "", "player2", true));

        assertEquals("Enter a team ID and a player username.", presenter.error);
        assertEquals(0, store.saveCount);
    }

    @Test
    void storageFailureIsReportedInsteadOfSuccess() {
        store.failOnSave = true;

        interactor.execute(new ManageRosterInputData("admin1", "t1", "player2", true));

        assertNull(presenter.success);
        assertEquals("Could not save the team: disk full", presenter.error);
    }

    private static final class FakeStore implements ManageRosterDataAccessInterface {
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

    private static final class RecordingPresenter implements ManageRosterOutputBoundary {
        private ManageRosterOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ManageRosterOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
