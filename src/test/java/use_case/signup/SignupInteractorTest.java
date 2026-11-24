package use_case.signup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryUserDataAccessObject;
import entity.Role;
import entity.User;
import entity.UserFactory;

/**
 * Tests the Sign Up use case: success, and every way it can fail.
 */
class SignupInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private RecordingPresenter presenter;
    private SignupInteractor interactor;

    /** Captures whatever the interactor asks the presenter to show. */
    private static class RecordingPresenter implements SignupOutputBoundary {

        private SignupOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(SignupOutputData outputData) {
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
        presenter = new RecordingPresenter();
        interactor = new SignupInteractor(dataAccess, presenter, new UserFactory());
    }

    @Test
    void createsTheAccount() {
        interactor.execute(new SignupInputData("newbie", "password1", "password1", "New Bie", "PLAYER"));

        assertNull(presenter.error);
        assertEquals("newbie", presenter.success.getUsername());
        assertEquals(Role.PLAYER, dataAccess.get("newbie").getRole());
        assertEquals("New Bie", dataAccess.get("newbie").getDisplayName());
        assertTrue(dataAccess.get("newbie").matches("password1"));
    }

    @Test
    void spacesAroundTheUsernameAreTrimmed() {
        interactor.execute(new SignupInputData("  spaced  ", "password1", "password1", "Spaced", "COACH"));

        assertNull(presenter.error);
        assertEquals(Role.COACH, dataAccess.get("spaced").getRole());
    }

    @Test
    void aBlankDisplayNameFallsBackToTheUsername() {
        interactor.execute(new SignupInputData("noname", "password1", "password1", "   ", "PARENT"));

        assertNull(presenter.error);
        assertEquals("noname", dataAccess.get("noname").getDisplayName());
    }

    @Test
    void mismatchedPasswordsFail() {
        interactor.execute(new SignupInputData("newbie", "password1", "other", "New Bie", "PLAYER"));

        assertEquals("The two passwords do not match.", presenter.error);
        assertNull(dataAccess.get("newbie"));
    }

    @Test
    void anExistingUsernameFails() {
        dataAccess.save(new User("taken", "password1", "Taken", Role.COACH));

        interactor.execute(new SignupInputData("taken", "password1", "password1", "Impostor", "PLAYER"));

        assertEquals("That username is already taken.", presenter.error);
        assertEquals("Taken", dataAccess.get("taken").getDisplayName());
    }

    @Test
    void anUnknownRoleFails() {
        interactor.execute(new SignupInputData("newbie", "password1", "password1", "New Bie", "WIZARD"));

        assertEquals("Choose a role from the list.", presenter.error);
        assertNull(dataAccess.get("newbie"));
    }

    @Test
    void blankFieldsFail() {
        interactor.execute(new SignupInputData("", "", "", "", "PLAYER"));

        assertEquals("Choose a username and a password.", presenter.error);
    }

    @Test
    void aShortPasswordIsRefusedWithTheRule() {
        interactor.execute(new SignupInputData("newbie", "pw", "pw", "New Bie", "PLAYER"));

        assertEquals("Passwords are 8 to 128 characters.", presenter.error);
        assertNull(dataAccess.get("newbie"));
    }

    @Test
    void aUsernameWithSpacesIsRefusedWithTheRule() {
        interactor.execute(new SignupInputData("new bie", "password1", "password1",
                "New Bie", "PLAYER"));

        assertEquals("Use only letters, digits, dots, underscores and hyphens in a username.",
                presenter.error);
        assertNull(dataAccess.get("new bie"));
    }

    @Test
    void aBrokenStoreIsReportedInsteadOfThrown() {
        final SignupUserDataAccessInterface broken = new SignupUserDataAccessInterface() {
            @Override
            public boolean existsByName(String username) {
                throw new IllegalStateException("the file is unreadable");
            }

            @Override
            public void save(User user) {
                throw new IllegalStateException("the file is unreadable");
            }
        };

        new SignupInteractor(broken, presenter, new UserFactory())
                .execute(new SignupInputData("newbie", "password1", "password1", "New Bie", "PLAYER"));

        assertTrue(presenter.error.startsWith("Could not save the account:"));
        assertNull(presenter.success);
    }
}
