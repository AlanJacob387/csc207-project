package use_case.logout;

/**
 * Output boundary for the Log Out use case.
 */
public interface LogoutOutputBoundary {

    /**
     * Called when the user was signed out.
     *
     * @param outputData who was signed out
     */
    void prepareSuccessView(LogoutOutputData outputData);

    /**
     * Called when there was nobody signed in to log out.
     *
     * @param errorMessage a message to show on the login page
     */
    void prepareFailView(String errorMessage);
}