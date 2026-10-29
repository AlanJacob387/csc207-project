package use_case.signup;

/**
 * Output boundary for the Sign Up use case.
 */
public interface SignupOutputBoundary {

    /**
     * Called when the account was created.
     *
     * @param outputData the new account's details
     */
    void prepareSuccessView(SignupOutputData outputData);

    /**
     * Called when the account could not be created.
     *
     * @param errorMessage a message to show on the sign-up page
     */
    void prepareFailView(String errorMessage);
}
