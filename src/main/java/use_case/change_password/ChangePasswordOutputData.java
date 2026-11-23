package use_case.change_password;

/**
 * Output data for the Change Password use case.
 */
public class ChangePasswordOutputData {

    private final String username;
    private final String message;

    /**
     * Creates the output data.
     *
     * @param username the user whose password changed
     * @param message  a message to show on the account page
     */
    public ChangePasswordOutputData(String username, String message) {
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