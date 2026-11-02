package use_case.logout;

import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Log Out use case.
 *
 * Clears the current user through the port, so the use case owns the session
 * transition instead of the view wiring doing it. Clearing an empty session is
 * reported as a failure rather than ignored, so the view can explain itself.
 */
public class LogoutInteractor implements LogoutInputBoundary {

    private final CurrentUserInterface currentUser;
    private final LogoutOutputBoundary presenter;

    /**
     * Creates the interactor.
     *
     * @param currentUser the port that records who is signed in
     * @param presenter   where the result is sent
     */
    public LogoutInteractor(CurrentUserInterface currentUser,
                            LogoutOutputBoundary presenter) {
        this.currentUser = currentUser;
        this.presenter = presenter;
    }

    @Override
    public void execute() {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isEmpty()) {
            presenter.prepareFailView("You are not signed in.");
            return;
        }

        currentUser.clearCurrentUser();
        presenter.prepareSuccessView(new LogoutOutputData(username));
    }
}