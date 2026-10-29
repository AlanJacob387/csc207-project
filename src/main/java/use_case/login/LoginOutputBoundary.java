package use_case.login;

/**
 * Output boundary for the Log In use case.
 */
public interface LoginOutputBoundary {

    /**
     * Called when the user logs in successfully.
     *
     * @param outputData the logged-in user's details
     */
    void prepareSuccessView(LoginOutputData outputData);

    /**
     * Called when the login cannot go ahead.
     *
     * @param errorMessage a message to show on the login page
     */
    void prepareFailView(String errorMessage);
}
