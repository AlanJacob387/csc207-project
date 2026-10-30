package use_case.signup;

/**
 * Output data for the Sign Up use case.
 */
public class SignupOutputData {

    private final String username;
    private final String message;

    /**
     * Creates the output data.
     *
     * @param username the new account's username
     * @param message  a message to show on the login page
     */
    public SignupOutputData(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }
}
