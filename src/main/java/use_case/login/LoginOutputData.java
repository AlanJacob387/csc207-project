package use_case.login;

/**
 * Output data for the Log In use case: who logged in.
 *
 * The display name and role are not repeated here - the interactor already
 * records the whole user in the current-user port.
 */
public class LoginOutputData {

    private final String username;

    /**
     * Creates the output data.
     *
     * @param username the logged-in username
     */
    public LoginOutputData(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}