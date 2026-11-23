package use_case.manage_users;

/**
 * Input for changing one account's role.
 */
public class ChangeUserRoleInputData {

    private final String username;
    private final String roleName;

    /**
     * Creates the input.
     *
     * @param username the account to change
     * @param roleName the name of the role to assign
     */
    public ChangeUserRoleInputData(String username, String roleName) {
        this.username = username;
        this.roleName = roleName;
    }

    public String getUsername() {
        return username;
    }

    public String getRoleName() {
        return roleName;
    }
}
