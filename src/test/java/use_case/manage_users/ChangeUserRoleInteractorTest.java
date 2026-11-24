package use_case.manage_users;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * Tests the Change User Role use case, including the two rules that keep the team
 * from locking itself out of the administrative pages.
 */
class ChangeUserRoleInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private Session session;
    private RecordingPresenter presenter;
    private ChangeUserRoleInteractor interactor;

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
        interactor = new ChangeUserRoleInteractor(dataAccess, presenter, session);
    }

    @Test
    void anAdministratorChangesAnotherAccountsRole() {
        interactor.execute(new ChangeUserRoleInputData("player1", "COACH"));

        assertNull(presenter.error);
        assertEquals(Role.COACH, dataAccess.get("player1").getRole());
        assertEquals("Role changed: player1 is now COACH.", presenter.success.getMessage());
    }

    @Test
    void aRoleChangeKeepsTheStoredCredential() {
        dataAccess.save(new User("player1", "pbkdf2$1000$c2FsdA==$aGFzaA==", "Pat",
                Role.PLAYER));

        interactor.execute(new ChangeUserRoleInputData("player1", "PARENT"));

        assertEquals("pbkdf2$1000$c2FsdA==$aGFzaA==", dataAccess.get("player1").getPassword());
        assertEquals("Pat", dataAccess.get("player1").getDisplayName());
        assertTrue(dataAccess.get("player1").isActive());
    }

    @Test
    void onlyAnAdministratorMayChangeRoles() {
        session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));

        interactor.execute(new ChangeUserRoleInputData("player1", "COACH"));

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error);
        assertNull(presenter.success);
        assertEquals(Role.PLAYER, dataAccess.get("player1").getRole());
    }

    @Test
    void anUnknownAccountIsRefused() {
        interactor.execute(new ChangeUserRoleInputData("ghost", "COACH"));

        assertEquals("No account found for ghost.", presenter.error);
    }

    @Test
    void youCannotChangeYourOwnRole() {
        interactor.execute(new ChangeUserRoleInputData("admin1", "COACH"));

        assertEquals("You cannot change your own role.", presenter.error);
        assertNull(presenter.success);
    }

    @Test
    void aRoleThatIsAlreadySetIsRefused() {
        interactor.execute(new ChangeUserRoleInputData("player1", "PLAYER"));

        assertEquals("player1 already has the PLAYER role.", presenter.error);
    }

    @Test
    void anUnknownRoleIsRefused() {
        interactor.execute(new ChangeUserRoleInputData("player1", "CAPTAIN"));

        assertEquals("Choose a role from the list.", presenter.error);
        assertEquals(Role.PLAYER, dataAccess.get("player1").getRole());
    }

    @Test
    void aDemotedAdministratorCannotActWithTheOldSessionRole() {
        dataAccess.save(new User("admin2", "pw", "Ada Two", Role.PLAYER));
        final Session staleSession = new Session();
        staleSession.setCurrentUser(new User("admin2", "pw", "Ada Two", Role.ADMIN));
        final ChangeUserRoleInteractor stale =
                new ChangeUserRoleInteractor(dataAccess, presenter, staleSession);

        stale.execute(new ChangeUserRoleInputData("player1", "COACH"));

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error,
                "the stored account is the authority, not the role the session remembers");
        assertEquals(Role.PLAYER, dataAccess.get("player1").getRole());
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
            public void createUser(User user) {
                throw new AssertionError("nothing should be created");
            }

            @Override
            public void save(User user) {
                throw new AssertionError("nothing should be saved");
            }
        };

        new ChangeUserRoleInteractor(broken, presenter, session)
                .execute(new ChangeUserRoleInputData("player1", "COACH"));

        assertEquals("Could not update the account: the store is unreadable", presenter.error);
    }
}
