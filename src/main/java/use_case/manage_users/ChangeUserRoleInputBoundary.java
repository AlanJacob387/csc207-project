package use_case.manage_users;

/**
 * Input boundary for the Change User Role use case.
 */
public interface ChangeUserRoleInputBoundary {

    /**
     * Changes a role, or explains why it could not be changed.
     *
     * @param inputData which account, and which role
     */
    void execute(ChangeUserRoleInputData inputData);
}
