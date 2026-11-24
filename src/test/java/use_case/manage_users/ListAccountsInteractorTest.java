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
 * Tests the read use case that fills the administrator page, including the rule
 * that the account list is as protected as the actions.
 */
class ListAccountsInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private Session session;
    private RecordingPresenter presenter;
    private ListAccountsInteractor interactor;

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
        session = new Session();
        session.setCurrentUser(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        presenter = new RecordingPresenter();
        interactor = new ListAccountsInteractor(dataAccess, presenter, session);
    }

    @Test
    void anAdministratorSeesEveryAccountSortedByUsername() {
        interactor.execute();

        assertNull(presenter.error);
        assertEquals(2, presenter.success.getAccounts().size());
        assertEquals("admin1", presenter.success.getAccounts().get(0).getUsername());
        assertEquals("player1", presenter.success.getAccounts().get(1).getUsername());
        assertEquals("", presenter.success.getMessage(), "a plain read has nothing to announce");
    }

    @Test
    void theRowsCarryTheRoleAndTheStatus() {
        dataAccess.save(new User("gone1", "pw", "Gone", Role.PARENT, false));

        interactor.execute();

        final ManageUsersOutputData.AccountData gone = presenter.success.getAccounts().stream()
                .filter(account -> "gone1".equals(account.getUsername()))
                .findFirst().orElseThrow();
        assertEquals(Role.PARENT, gone.getRole());
        assertEquals("Gone", gone.getDisplayName());
        assertFalse(gone.isActive());
    }

    @Test
    void onlyAnAdministratorMayReadTheAccountList() {
        session.setCurrentUser(new User("player1", "pw", "Pat Player", Role.PLAYER));

        interactor.execute();

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error);
        assertNull(presenter.success, "no accounts may leak to a non-administrator");
    }

    @Test
    void aSignedOutCallerIsRefused() {
        session.clearCurrentUser();

        interactor.execute();

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error);
    }

    @Test
    void aBrokenStoreIsReportedInsteadOfThrown() {
        final ManageUsersDataAccessInterface broken = new ManageUsersDataAccessInterface() {
            @Override
            public List<User> getAllUsers() {
                throw new IllegalStateException("the store is unreadable");
            }

            @Override
            public User get(String username) {
                return new User("admin1", "pw", "Ada Admin", Role.ADMIN);
            }

            @Override
            public boolean existsByName(String username) {
                return false;
            }

            @Override
            public void createUser(User user) {
                throw new AssertionError("nothing should be created");
            }

            @Override
            public void save(User user) {
                throw new AssertionError("nothing should be saved");
            }
        };

        new ListAccountsInteractor(broken, presenter, session).execute();

        assertEquals("Could not read the accounts: the store is unreadable", presenter.error);
        assertTrue(presenter.error.startsWith("Could not read the accounts:"));
    }
}
