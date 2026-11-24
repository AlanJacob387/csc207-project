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
            public void save(User user) {
                throw new AssertionError("nothing should be saved");
            }
        };

        new DeactivateUserInteractor(broken, presenter, session)
                .execute(new DeactivateUserInputData("player1"));

        assertEquals("Could not update the account: the store is unreadable", presenter.error);
    }
}
