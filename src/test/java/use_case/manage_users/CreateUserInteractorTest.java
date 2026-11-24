package use_case.manage_users;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryUserDataAccessObject;
import entity.Role;
import entity.User;
import entity.UserFactory;
import interface_adapter.Session;
import use_case.accounts.DuplicateUsernameException;

/**
 * Tests the Create User use case: an administrator creates an account, and every
 * way the action is refused.
 */
class CreateUserInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private Session session;
    private RecordingPresenter presenter;
    private CreateUserInteractor interactor;

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
        session = new Session();
        session.setCurrentUser(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        presenter = new RecordingPresenter();
        interactor = new CreateUserInteractor(dataAccess, presenter, session, new UserFactory());
    }

    private static CreateUserInputData input(String username, String displayName, String password,
                                             String repeatPassword, Role role) {
        return new CreateUserInputData(username, displayName, password, repeatPassword,
                role.name());
    }

    @Test
    void anAdministratorCreatesAnAccountWithTheChosenRole() {
        interactor.execute(input("player9", "Pat Nine", "secretpass", "secretpass", Role.PLAYER));

        assertNull(presenter.error);
        assertEquals(Role.PLAYER, dataAccess.get("player9").getRole());
        assertEquals("Pat Nine", dataAccess.get("player9").getDisplayName());
        assertEquals("Account created for player9.", presenter.success.getMessage());
        assertTrue(presenter.success.getAccounts().stream()
                        .anyMatch(account -> "player9".equals(account.getUsername())),
                "the refreshed list should include the new account");
    }

    @Test
    void theChosenPasswordIsStoredHashed() {
        interactor.execute(input("player9", "", "secretpass", "secretpass", Role.PLAYER));

        assertTrue(dataAccess.get("player9").matches("secretpass"));
        assertNotEquals("secretpass", dataAccess.get("player9").getPassword());
    }

    @Test
    void aBlankDisplayNameFallsBackToTheUsername() {
        interactor.execute(input("player9", "   ", "secretpass", "secretpass", Role.PLAYER));

        assertEquals("player9", dataAccess.get("player9").getDisplayName());
    }

    @Test
    void onlyAnAdministratorMayCreateAccounts() {
        session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));

        interactor.execute(input("player9", "", "secretpass", "secretpass", Role.PLAYER));

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error);
        assertNull(presenter.success);
        assertNull(dataAccess.get("player9"));
    }

    @Test
    void aSignedOutCallerIsRefused() {
        session.clearCurrentUser();

        interactor.execute(input("player9", "", "secretpass", "secretpass", Role.PLAYER));

        assertEquals(AdminPolicy.NOT_ADMIN, presenter.error);
    }

    @Test
    void aTakenUsernameIsRefused() {
        dataAccess.save(new User("player9", "pw", "Existing Player", Role.PLAYER));

        interactor.execute(input("player9", "Someone Else", "secretpass", "secretpass", Role.PLAYER));

        assertEquals("That username is already taken.", presenter.error);
        assertEquals("Existing Player", dataAccess.get("player9").getDisplayName(),
                "the existing account must not be touched");
    }

    @Test
    void aBlankUsernameOrPasswordIsRefused() {
        interactor.execute(input("", "", "secretpass", "secretpass", Role.PLAYER));
        assertEquals("Choose a username and a password.", presenter.error);

        interactor.execute(input("player9", "", "", "", Role.PLAYER));
        assertEquals("Choose a username and a password.", presenter.error);
        assertNull(dataAccess.get("player9"));
    }

    @Test
    void mismatchedPasswordsAreRefused() {
        interactor.execute(input("player9", "", "secretpass", "secret2", Role.PLAYER));

        assertEquals("The two passwords do not match.", presenter.error);
        assertNull(dataAccess.get("player9"));
    }

    @Test
    void anUnknownRoleIsRefused() {
        interactor.execute(new CreateUserInputData("player9", "", "secretpass", "secretpass", "CAPTAIN"));

        assertEquals("Choose a role from the list.", presenter.error);
        assertNull(dataAccess.get("player9"));
    }

    @Test
    void anOverlongUsernameIsRefused() {
        interactor.execute(input("x".repeat(41), "", "secretpass", "secretpass", Role.PLAYER));

        assertTrue(presenter.error.contains("3 to 40"));
        assertNull(dataAccess.get("x".repeat(41)));
    }

    @Test
    void aPasswordShorterThanTheRuleIsRefused() {
        interactor.execute(input("player9", "Pat Nine", "pw", "pw", Role.PLAYER));

        assertEquals("Passwords are 8 to 128 characters.", presenter.error);
        assertNull(dataAccess.get("player9"));
    }

    @Test
    void aUsernameClaimedBeforeTheWriteIsReported() {
        final User admin = new User("admin1", "pw", "Ada Admin", Role.ADMIN);
        final CreateUserDataAccessInterface racy = new CreateUserDataAccessInterface() {
            @Override
            public User get(String username) {
                return admin;
            }

            @Override
            public List<User> getAllUsers() {
                return List.of(admin);
            }

            @Override
            public boolean existsByName(String username) {
                return false;
            }

            @Override
            public void createUser(User user) {
                throw new DuplicateUsernameException(user.getUsername());
            }
        };

        new CreateUserInteractor(racy, presenter, session, new UserFactory())
                .execute(input("player9", "Pat Nine", "secretpass", "secretpass", Role.PLAYER));

        assertEquals("That username is already taken.", presenter.error);
        assertNull(presenter.success);
    }

    @Test
    void aBrokenStoreIsReportedInsteadOfThrown() {
        final CreateUserDataAccessInterface broken = new CreateUserDataAccessInterface() {
            @Override
            public User get(String username) {
                return "admin1".equals(username)
                        ? new User("admin1", "pw", "Ada Admin", Role.ADMIN) : null;
            }

            @Override
            public List<User> getAllUsers() {
                return List.of();
            }

            @Override
            public boolean existsByName(String username) {
                return false;
            }

            @Override
            public void createUser(User user) {
                throw new IllegalStateException("the store is unwritable");
            }
        };

        new CreateUserInteractor(broken, presenter, session, new UserFactory())
                .execute(input("player9", "", "secretpass", "secretpass", Role.PLAYER));

        assertEquals("Could not save the account: the store is unwritable", presenter.error);
        assertNull(presenter.success);
    }


    @Test
    void aListThatCannotBeReadBackDoesNotMakeASavedAccountLookUnsaved() {
        final RacingStore store = new RacingStore();
        store.save(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.failTheListAfterAWrite();

        new CreateUserInteractor(store, presenter, session, new UserFactory())
                .execute(input("player9", "Pat Nine", "secretpass", "secretpass", Role.PLAYER));

        assertNotNull(store.get("player9"), "the account was stored");
        assertNull(presenter.success);
        assertTrue(presenter.error.startsWith("Account created for player9."), presenter.error);
        assertTrue(presenter.error.contains("It was saved"), presenter.error);
        assertFalse(presenter.error.startsWith("Could not save"), presenter.error);
    }
}
