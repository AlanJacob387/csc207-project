package use_case.manage_users;

/**
 * Input boundary for the Deactivate User use case.
 */
public interface DeactivateUserInputBoundary {

    /**
     * Deactivates an account, or explains why it could not be deactivated.
     *
     * @param inputData which account
     */
    void execute(DeactivateUserInputData inputData);
}
