package interface_adapter.manage_users;

import use_case.manage_users.ChangeUserRoleInputBoundary;
import use_case.manage_users.ChangeUserRoleInputData;

/**
 * Controller for Change User Role. It builds the input data and decides nothing.
 */
public class ChangeUserRoleController {

    private final ChangeUserRoleInputBoundary changeUserRole;

    /**
     * Creates the controller.
     *
     * @param changeUserRole the Change User Role use case
     */
    public ChangeUserRoleController(ChangeUserRoleInputBoundary changeUserRole) {
        this.changeUserRole = changeUserRole;
    }

    /**
     * Changes one account's role.
     *
     * @param username the account to change
     * @param roleName the name of the role to assign
     */
    public void execute(String username, String roleName) {
        changeUserRole.execute(new ChangeUserRoleInputData(username, roleName));
    }
}
