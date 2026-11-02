package use_case.signup;

/**
 * Input data for the Sign Up use case.
 */
public class SignupInputData {

    private final String username;
    private final String password;
    private final String repeatPassword;
    private final String displayName;
    private final String roleName;

    /**
     * Creates the input data.
     *
     * @param username       the chosen username
     * @param password       the chosen password
     * @param repeatPassword the password typed a second time
     * @param displayName    the name to show in the app (may be blank)
     * @param roleName       the chosen role, as shown in the dropdown
     */
    public SignupInputData(String username, String password, String repeatPassword,
                           String displayName, String roleName) {
        this.username = username;
        this.password = password;
        this.repeatPassword = repeatPassword;
        this.displayName = displayName;
        this.roleName = roleName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRepeatPassword() {
        return repeatPassword;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoleName() {
        return roleName;
    }
}
