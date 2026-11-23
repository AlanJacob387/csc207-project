package use_case.manage_users;

/**
 * Input boundary for the Reactivate User use case.
 */
public interface ReactivateUserInputBoundary {

    /**
     * Reactivates an account, or explains why it could not be reactivated.
     *
     * @param inputData which account
     */
    void execute(ReactivateUserInputData inputData);
}
