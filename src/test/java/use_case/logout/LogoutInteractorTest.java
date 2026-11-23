package use_case.logout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Role;
import entity.User;
import interface_adapter.Session;

/**
 * Tests the Log Out use case against the real session state, which is what
 * implements the current-user port.
 */
class LogoutInteractorTest {

    private Session session;
    private RecordingPresenter presenter;
    private LogoutInteractor interactor;

    /** Captures whatever the interactor asks the presenter to show. */
    private static class RecordingPresenter implements LogoutOutputBoundary {

        private LogoutOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(LogoutOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        session = new Session();
        presenter = new RecordingPresenter();
        interactor = new LogoutInteractor(session, presenter);
    }

    @Test
    void loggingOutClearsTheSessionAndReportsWhoLeft() {
        session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));

        interactor.execute();

        assertNull(presenter.error);
        assertEquals("coach1", presenter.success.getUsername());
        assertEquals("", session.getCurrentUsername());
        assertNull(session.getCurrentRole());
        assertFalse(session.isLoggedIn());
    }

    @Test
    void loggingOutWithAnEmptySessionFails() {
        interactor.execute();

        assertEquals("You are not signed in.", presenter.error);
        assertNull(presenter.success);
        assertFalse(session.isLoggedIn());
    }
}
