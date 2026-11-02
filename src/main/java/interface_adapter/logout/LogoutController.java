package interface_adapter.logout;

import use_case.logout.LogoutInputBoundary;

/**
 * Controller for the Log Out use case.
 * Called by the app shell when the Log out button is pressed.
 */
public class LogoutController {

    private final LogoutInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the Log Out use case
     */
    public LogoutController(LogoutInputBoundary interactor) {
        this.interactor = interactor;
    }

    /** Signs the current user out. */
    public void execute() {
        interactor.execute();
    }
}