package use_case.manage_users;

/**
 * Input for creating an account from the administrator page.
 */
public class CreateUserInputData {

    private final String username;
    private final String displayName;
    private final String password;
    private final String repeatPassword;
    private final String roleName;

    /**
     * Creates the input.
     *
     * @param username       the username for the new account
     * @param displayName    the name to show, or empty to reuse the username
     * @param password       the chosen password
     * @param repeatPassword the same password typed again
     * @param roleName       the name of the chosen role
     */
    public CreateUserInputData(String username, String displayName, String password,
                               String repeatPassword, String roleName) {
        this.username = username;
        this.displayName = displayName;
        this.password = password;
        this.repeatPassword = repeatPassword;
        this.roleName = roleName;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPassword() {
        return password;
    }

    public String getRepeatPassword() {
        return repeatPassword;
    }

    public String getRoleName() {
        return roleName;
    }
}
