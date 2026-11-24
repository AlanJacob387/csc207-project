package use_case.manage_users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Tests the Reactivate User use case, which makes deactivation reversible.
 */
class ReactivateUserInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private Session session;
    private RecordingPresenter presenter;
    private ReactivateUserInteractor interactor;

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
        dataAccess.save(new User("player1", "hashed-secret", "Pat Player", Role.PLAYER, false));
        session = new Session();
        session.setCurrentUser(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        presenter = new RecordingPresenter();
        interactor = new ReactivateUserInteractor(dataAccess, presenter, session);
    }

    @Test
    void anAdministratorReactivatesAnAccount() {
        interactor.execute(new ReactivateUserInputData("player1"));

        assertNull(presenter.error);
        assertTrue(dataAccess.get("player1").isActive());
        assertEquals("Account reactivated: player1.", presenter.success.getMessage());
        assertTrue(presenter.success.getAccounts().stream()
                        .filter(account -> "player1".equals(account.getUsername()))
                        .anyMatch(ManageUsersOutputData.AccountData::isActive),
                "the refreshed list should show the account as active");
    }

    @Test
    void theCredentialAndProfileSurvive() {
        interactor.execute(new ReactivateUserInputData("player1"));

        assertEquals("hashed-secret", dataAccess.get("player1").getPassword());
        assertEquals("Pat Player", dataAccess.get("player1").getDisplayName());
        assertEquals(Role.PLAYER, dataAccess.get("player1").getRole());
    }

    @Test
    void onlyAnAdministratorMayReactivateAccounts() {
        session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));

        interactor.execute(new ReactivateUserInputData("player1"));

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error);
        assertFalse(dataAccess.get("player1").isActive());
    }

    @Test
    void anUnknownAccountIsRefused() {
        interactor.execute(new ReactivateUserInputData("ghost"));

        assertEquals("No account found for ghost.", presenter.error);
    }

    @Test
    void anAccountThatIsAlreadyActiveIsRefused() {
        interactor.execute(new ReactivateUserInputData("admin1"));

        assertEquals("admin1 is already active.", presenter.error);
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
                return new User("player1", "pw", "Pat", Role.PLAYER, false);
            }

            @Override
            public boolean existsByName(String username) {
                return false;
            }

            @Override
            public void createUser(User user) {
                throw new IllegalStateException("the store is unwritable");
            }

            @Override
            public void save(User user) {
                throw new IllegalStateException("the store is unwritable");
            }
        };

        new ReactivateUserInteractor(broken, presenter, session)
                .execute(new ReactivateUserInputData("player1"));

        assertEquals("Could not update the account: the store is unwritable", presenter.error);
    }
}
