package use_case.manage_users;

/**
 * Input boundary for the Create User use case.
 */
public interface CreateUserInputBoundary {

    /**
     * Creates an account, or explains why it could not be created.
     *
     * @param inputData the details typed on the administrator page
     */
    void execute(CreateUserInputData inputData);
}
