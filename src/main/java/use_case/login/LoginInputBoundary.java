package use_case.login;

/**
 * Input boundary for the Log In use case.
 */
public interface LoginInputBoundary {

    /**
     * Attempts to log a user in.
     *
     * @param inputData the username and password entered by the user
     */
    void execute(LoginInputData inputData);
}
