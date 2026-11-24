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
 * Tests the authorization rules directly: who may manage users, and when an action
 * would leave no active administrator.
 *
 * The "last active administrator" rule is tested here rather than through an
 * interactor because a verified administrator cannot act on their own account, so
 * the page can only reach that state when two administrators act at the same
 * moment.
 */
class AdminPolicyTest {

    private InMemoryUserDataAccessObject dataAccess;
    private Session session;

    @BeforeEach
    void setUp() {
        dataAccess = new InMemoryUserDataAccessObject();
        dataAccess.save(new User("admin1", "pw", "Ada One", Role.ADMIN));
        dataAccess.save(new User("admin2", "pw", "Ada Two", Role.ADMIN));
        dataAccess.save(new User("player1", "pw", "Pat", Role.PLAYER));
        session = new Session();
        session.setCurrentUser(new User("admin1", "pw", "Ada One", Role.ADMIN));
    }

    @Test
    void anActiveStoredAdministratorIsAllowed() {
        assertNull(AdminPolicy.refusalFor(session, dataAccess));
    }

    @Test
    void aSignedOutCallerIsRefusedWithoutReadingTheStore() {
        session.clearCurrentUser();

        assertEquals(AdminPolicy.NOT_ADMIN, AdminPolicy.refusalFor(session, unreadableStore()));
    }

    @Test
    void aNonAdministratorIsRefusedWithoutReadingTheStore() {
        session.setCurrentUser(new User("player1", "pw", "Pat", Role.PLAYER));

        assertEquals(AdminPolicy.NOT_ADMIN, AdminPolicy.refusalFor(session, unreadableStore()));
    }

    @Test
    void aDemotedAdministratorIsRefusedBecauseTheSessionIsStale() {
        dataAccess.save(new User("admin2", "pw", "Ada Two", Role.PLAYER));
        session.setCurrentUser(new User("admin2", "pw", "Ada Two", Role.ADMIN));

        assertEquals(AdminPolicy.NOT_ADMIN, AdminPolicy.refusalFor(session, dataAccess),
                "the stored account is the authority, not the role the session remembers");
    }

    @Test
    void aDeactivatedAdministratorIsRefusedBecauseTheSessionIsStale() {
        dataAccess.save(new User("admin2", "pw", "Ada Two", Role.ADMIN, false));
        session.setCurrentUser(new User("admin2", "pw", "Ada Two", Role.ADMIN));

        assertEquals(AdminPolicy.NOT_ADMIN, AdminPolicy.refusalFor(session, dataAccess));
    }

    @Test
    void anAdministratorWhoIsNoLongerStoredIsRefused() {
        session.setCurrentUser(new User("ghost", "pw", "Gone", Role.ADMIN));

        assertEquals(AdminPolicy.NOT_ADMIN, AdminPolicy.refusalFor(session, dataAccess));
    }

    @Test
    void aStoreThatCannotBeReadIsARefusalRatherThanAPass() {
        final String refusal = AdminPolicy.refusalFor(session, unreadableStore());

        assertTrue(refusal.startsWith(AdminPolicy.UNCONFIRMED), refusal);
        assertTrue(refusal.contains("the store is unreadable"), refusal);
    }

    @Test
    void onlyOneActiveAdministratorMeansTheTargetIsTheLast() {
        assertTrue(AdminPolicy.isLastActiveAdmin(
                List.of(new User("admin2", "pw", "Ada Two", Role.ADMIN)),
                new User("admin2", "pw", "Ada Two", Role.ADMIN)));
    }

    @Test
    void aSecondActiveAdministratorIsAReplacement() {
        assertFalse(AdminPolicy.isLastActiveAdmin(List.of(
                new User("admin1", "pw", "Ada One", Role.ADMIN),
                new User("admin2", "pw", "Ada Two", Role.ADMIN)),
                new User("admin2", "pw", "Ada Two", Role.ADMIN)));
    }

    @Test
    void anInactiveAdministratorDoesNotCountAsAReplacement() {
        assertTrue(AdminPolicy.isLastActiveAdmin(List.of(
                new User("ada2", "pw", "Ada Two", Role.ADMIN),
                new User("ada3", "pw", "Ada Three", Role.ADMIN, false)),
                new User("ada2", "pw", "Ada Two", Role.ADMIN)),
                "an inactive administrator cannot sign in, so it is no replacement");
    }

    @Test
    void anInactiveOrNonAdministratorTargetIsNeverTheLast() {
        assertFalse(AdminPolicy.isLastActiveAdmin(
                List.of(new User("ada3", "pw", "Ada Three", Role.ADMIN, false)),
                new User("ada3", "pw", "Ada Three", Role.ADMIN, false)));
        assertFalse(AdminPolicy.isLastActiveAdmin(
                List.of(new User("player1", "pw", "Pat", Role.PLAYER)),
                new User("player1", "pw", "Pat", Role.PLAYER)));
    }

    /** A store that fails the moment an administrator has to be confirmed. */
    private static ManageUsersDataAccessInterface unreadableStore() {
        return new ManageUsersDataAccessInterface() {
            @Override
            public List<User> getAllUsers() {
                throw new AssertionError("the account list is not needed here");
            }

            @Override
            public User get(String username) {
                throw new IllegalStateException("the store is unreadable");
            }

            @Override
            public boolean existsByName(String username) {
                throw new AssertionError("the account list is not needed here");
            }

            @Override
            public void save(User user) {
                throw new AssertionError("nothing may be written");
            }
        };
    }
}
