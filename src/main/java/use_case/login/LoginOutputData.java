package use_case.login;

import entity.Role;

/**
 * Output data for the Log In use case: enough about the user for the shell to
 * greet them and decide which pages they may see.
 */
public class LoginOutputData {

    private final String username;
    private final String displayName;
    private final Role role;

    /**
     * Creates the output data.
     *
     * @param username    the logged-in username
     * @param displayName the name to greet the user with
     * @param role        the user's role
     */
    public LoginOutputData(String username, String displayName, Role role) {
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }
}
