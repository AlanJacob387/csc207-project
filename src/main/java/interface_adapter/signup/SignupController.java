package interface_adapter.signup;

import use_case.signup.SignupInputBoundary;
import use_case.signup.SignupInputData;

/**
 * Controller for the Sign Up use case.
 * Called by the view; converts raw input into use-case input data.
 */
public class SignupController {

    private final SignupInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the Sign Up use case
     */
    public SignupController(SignupInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Attempts to create an account.
     *
     * @param username       the chosen username
     * @param password       the chosen password
     * @param repeatPassword the password typed a second time
     * @param displayName    the name to show in the app
     * @param roleName       the chosen role, as shown in the dropdown
     */
    public void execute(String username, String password, String repeatPassword,
                        String displayName, String roleName) {
        interactor.execute(new SignupInputData(username, password, repeatPassword,
                displayName, roleName));
    }
}
