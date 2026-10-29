package use_case.signup;

/**
 * Input boundary for the Sign Up use case.
 */
public interface SignupInputBoundary {

    /**
     * Attempts to create an account.
     *
     * @param inputData the details entered on the sign-up page
     */
    void execute(SignupInputData inputData);
}
