package use_case.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryUserDataAccessObject;
import entity.Role;
import entity.User;

/**
 * Tests the Log In use case: success, and every way it can fail.
 */
class LoginInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private RecordingPresenter presenter;
    private LoginInteractor interactor;

    /** Captures whatever the interactor asks the presenter to show. */
    private static class RecordingPresenter implements LoginOutputBoundary {

        private LoginOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(LoginOutputData outputData) {
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
        dataAccess.save(new User("coach1", "pw", "Coach One", Role.COACH));
        presenter = new RecordingPresenter();
        interactor = new LoginInteractor(dataAccess, presenter);
    }

    @Test
    void correctDetailsLogIn() {
        interactor.execute(new LoginInputData("coach1", "pw"));

        assertNull(presenter.error);
        assertEquals("coach1", presenter.success.getUsername());
        assertEquals("Coach One", presenter.success.getDisplayName());
        assertEquals(Role.COACH, presenter.success.getRole());
    }

    @Test
    void spacesAroundTheUsernameAreIgnored() {
        interactor.execute(new LoginInputData("   coach1   ", "pw"));

        assertNull(presenter.error);
        assertEquals("coach1", presenter.success.getUsername());
    }

    @Test
    void wrongPasswordFails() {
        interactor.execute(new LoginInputData("coach1", "nope"));

        assertEquals("Incorrect password.", presenter.error);
        assertNull(presenter.success);
    }

    @Test
    void unknownUsernameFails() {
        interactor.execute(new LoginInputData("ghost", "pw"));

        assertEquals("No account found for ghost.", presenter.error);
        assertNull(presenter.success);
    }

    @Test
    void blankFieldsFail() {
        interactor.execute(new LoginInputData("", "pw"));
        assertEquals("Enter both a username and a password.", presenter.error);

        interactor.execute(new LoginInputData("coach1", ""));
        assertEquals("Enter both a username and a password.", presenter.error);
    }

    @Test
    void nullFieldsFail() {
        interactor.execute(new LoginInputData(null, null));

        assertEquals("Enter both a username and a password.", presenter.error);
    }

    @Test
    void aBrokenStoreIsReportedInsteadOfThrown() {
        final LoginUserDataAccessInterface broken = new LoginUserDataAccessInterface() {
            @Override
            public User get(String username) {
                throw new IllegalStateException("the file is unreadable");
            }

            @Override
            public boolean existsByName(String username) {
                return false;
            }
        };

        new LoginInteractor(broken, presenter).execute(new LoginInputData("coach1", "pw"));

        assertTrue(presenter.error.startsWith("Could not read the account store:"));
        assertNull(presenter.success);
    }
}
