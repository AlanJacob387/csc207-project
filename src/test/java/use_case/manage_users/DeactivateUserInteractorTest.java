package use_case.manage_users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryUserDataAccessObject;
import entity.Role;
import entity.User;
import interface_adapter.Session;

/**
 * Tests the Deactivate User use case: the account is kept but refused at login,
 * and the two rules that stop an administrator locking the team out.
 */
class DeactivateUserInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private Session session;
    private RecordingPresenter presenter;
    private DeactivateUserInteractor interactor;

    /** Captures whatever the interactor asks the presenter to show. */
    private static class RecordingPresenter implements ManageUsersOutputBoundary {

        private ManageUsersOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ManageUsersOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new InMemoryUserDataAccessObject();
        dataAccess.save(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        dataAccess.save(new User("player1", "pw", "Pat Player", Role.PLAYER));
        dataAccess.save(new User("admin2", "pw", "Ada Two", Role.ADMIN));
        session = new Session();
        session.setCurrentUser(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        presenter = new RecordingPresenter();
        interactor = new DeactivateUserInteractor(dataAccess, presenter, session);
    }

    @Test
    void anAdministratorDeactivatesAnotherAccount() {
        interactor.execute(new DeactivateUserInputData("player1"));

        assertNull(presenter.error);
        assertFalse(dataAccess.get("player1").isActive());
        assertEquals("Account deactivated: player1.", presenter.success.getMessage());
        assertTrue(presenter.success.getAccounts().stream()
                        .filter(account -> "player1".equals(account.getUsername()))
                        .noneMatch(ManageUsersOutputData.AccountData::isActive),
                "the refreshed list should show the account as deactivated");
    }

    @Test
    void theAccountIsKeptRatherThanDeleted() {
        interactor.execute(new DeactivateUserInputData("player1"));

        assertNotNull(dataAccess.get("player1"), "history refers to the account, so it stays");
        assertTrue(dataAccess.get("player1").matches("pw"), "the credential is untouched");
    }

    @Test
    void onlyAnAdministratorMayDeactivateAccounts() {
        session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));

        interactor.execute(new DeactivateUserInputData("player1"));

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error);
        assertTrue(dataAccess.get("player1").isActive());
    }

    @Test
    void anUnknownAccountIsRefused() {
        interactor.execute(new DeactivateUserInputData("ghost"));

        assertEquals("No account found for ghost.", presenter.error);
    }

    @Test
    void youCannotDeactivateYourself() {
        interactor.execute(new DeactivateUserInputData("admin1"));

        assertEquals("You cannot deactivate your own account.", presenter.error);
        assertNull(presenter.success);
    }

    @Test
    void anAlreadyDeactivatedAccountIsRefused() {
        dataAccess.save(new User("player1", "pw", "Pat Player", Role.PLAYER, false));

        interactor.execute(new DeactivateUserInputData("player1"));

        assertEquals("player1 is already deactivated.", presenter.error);
    }

    @Test
    void aDeactivatedAdministratorCannotActWithTheOldSessionRole() {
        dataAccess.save(new User("admin2", "pw", "Ada Two", Role.ADMIN, false));
        final Session staleSession = new Session();
        staleSession.setCurrentUser(new User("admin2", "pw", "Ada Two", Role.ADMIN));
        final DeactivateUserInteractor stale =
                new DeactivateUserInteractor(dataAccess, presenter, staleSession);

        stale.execute(new DeactivateUserInputData("player1"));

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error,
                "an account that was deactivated elsewhere must lose its powers");
        assertTrue(dataAccess.get("player1").isActive());
    }

    @Test
    void aBrokenStoreIsReportedInsteadOfThrown() {
        final ManageUsersDataAccessInterface broken = new ManageUsersDataAccessInterface() {
            @Override
            public List<User> getAllUsers() {
                return List.of();
            }

            @Override
            public User get(String username) {
                if ("admin1".equals(username)) {
                    return new User("admin1", "pw", "Ada Admin", Role.ADMIN);
                }
                throw new IllegalStateException("the store is unreadable");
            }

            @Override
            public boolean existsByName(String username) {
                return false;
            }

            @Override
            public void updateRole(String username, Role role) {
                throw new AssertionError("nothing should be saved");
            }

            @Override
            public void updateActive(String username, boolean active) {
                throw new AssertionError("nothing should be saved");
            }
        };

        new DeactivateUserInteractor(broken, presenter, session)
                .execute(new DeactivateUserInputData("player1"));

        assertEquals("Could not update the account: the store is unreadable", presenter.error);
    }


    @Test
    void aListThatCannotBeReadBackDoesNotMakeASavedChangeLookFailed() {
        final RacingStore store = seeded();
        store.failTheListAfterAWrite();

        new DeactivateUserInteractor(store, presenter, session)
                .execute(new DeactivateUserInputData("player1"));

        assertFalse(store.get("player1").isActive(), "the change was written");
        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Account deactivated: player1."), presenter.error);
        assertTrue(presenter.error.contains("It was saved"), presenter.error);
        assertFalse(presenter.error.startsWith("Could not update"), presenter.error);
    }

    @Test
    void aDeactivationThatRacesAnotherOneIsTakenBackSoSomeoneCanStillAdminister() {
        final RacingStore store = seeded();
        // admin2 deactivates admin1 in the same moment admin1 deactivates admin2.
        store.runAfterTheFirstWrite(() -> store.save(
                new User("admin1", "pw", "Ada Admin", Role.ADMIN, false)));

        new DeactivateUserInteractor(store, presenter, session)
                .execute(new DeactivateUserInputData("admin2"));

        assertEquals(AdminPolicy.LAST_ADMIN, presenter.error);
        assertNull(presenter.success);
        assertTrue(store.get("admin2").isActive(), "the write that left nobody is undone");
    }

    @Test
    void ifTheUndoFailsTheAdministratorIsToldWhoIsStillDeactivated() {
        final RacingStore store = seeded();
        store.runAfterTheFirstWrite(() -> store.save(
                new User("admin1", "pw", "Ada Admin", Role.ADMIN, false)));
        store.failWritesFrom(2);

        new DeactivateUserInteractor(store, presenter, session)
                .execute(new DeactivateUserInputData("admin2"));

        assertTrue(presenter.error.startsWith(AdminPolicy.LAST_ADMIN), presenter.error);
        assertTrue(presenter.error.contains("admin2 is deactivated and could not be restored"),
                presenter.error);
    }

    @Test
    void aCredentialChangedAfterTheReadIsNotWrittenBack() {
        final InMemoryUserDataAccessObject store = new InMemoryUserDataAccessObject() {
            @Override
            public User get(String username) {
                // What the administrator read a moment before player1 changed password.
                return "player1".equals(username)
                        ? new User("player1", "old-hash", "Pat Player", Role.PLAYER)
                        : super.get(username);
            }
        };
        store.save(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.save(new User("admin2", "pw", "Ada Two", Role.ADMIN));
        store.save(new User("player1", "new-hash", "Pat Player", Role.PLAYER));

        new DeactivateUserInteractor(store, presenter, session)
                .execute(new DeactivateUserInputData("player1"));

        assertNull(presenter.error);
        assertEquals("new-hash", store.getAllUsers().stream()
                        .filter(user -> "player1".equals(user.getUsername()))
                        .findFirst().orElseThrow().getPassword(),
                "only the status is written, so the newer credential survives");
    }

    private RacingStore seeded() {
        final RacingStore store = new RacingStore();
        store.save(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.save(new User("player1", "pw", "Pat Player", Role.PLAYER));
        store.save(new User("admin2", "pw", "Ada Two", Role.ADMIN));
        return store;
    }
}
