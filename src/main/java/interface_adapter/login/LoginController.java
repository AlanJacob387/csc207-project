package interface_adapter.login;

import use_case.login.LoginInputBoundary;
import use_case.login.LoginInputData;

/**
 * Controller for the Log In use case.
 * Called by the view; converts raw input into use-case input data.
 */
public class LoginController {

    private final LoginInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the Log In use case
     */
    public LoginController(LoginInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Attempts a login.
     *
     * @param username the entered username
     * @param password the entered password
     */
    public void execute(String username, String password) {
        interactor.execute(new LoginInputData(username, password));
    }
}
