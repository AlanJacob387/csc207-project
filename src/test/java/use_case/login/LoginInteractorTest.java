package use_case.login;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryUserDataAccessObject;
import entity.Role;
import entity.User;
import interface_adapter.Session;

/**
 * Tests the Log In use case: success (which records the current user) and every
 * way it can fail.
 */
class LoginInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private Session session;
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
        session = new Session();
        presenter = new RecordingPresenter();
        interactor = new LoginInteractor(dataAccess, presenter, session);
    }

    @Test
    void correctDetailsLogInAndRecordTheCurrentUser() {
        interactor.execute(new LoginInputData("coach1", "pw"));

        assertNull(presenter.error);
        assertEquals("coach1", presenter.success.getUsername());
        assertEquals("coach1", session.getCurrentUsername());
        assertEquals("Coach One", session.getDisplayName());
        assertEquals(Role.COACH, session.getRole());
        assertTrue(session.isLoggedIn());
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
    void aFailedLoginRecordsNobody() {
        interactor.execute(new LoginInputData("coach1", "nope"));

        assertEquals("", session.getCurrentUsername());
        assertNull(session.getRole());
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
        };

        new LoginInteractor(broken, presenter, session)
                .execute(new LoginInputData("coach1", "pw"));

        assertTrue(presenter.error.startsWith("Could not read the account store:"));
        assertNull(presenter.success);
    }
}