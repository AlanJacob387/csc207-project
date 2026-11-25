package interface_adapter.manage_users;

import use_case.manage_users.ReactivateUserInputBoundary;
import use_case.manage_users.ReactivateUserInputData;

/**
 * Controller for Reactivate User. It builds the input data and decides nothing.
 */
public class ReactivateUserController {

    private final ReactivateUserInputBoundary reactivateUser;

    /**
     * Creates the controller.
     *
     * @param reactivateUser the Reactivate User use case
     */
    public ReactivateUserController(ReactivateUserInputBoundary reactivateUser) {
        this.reactivateUser = reactivateUser;
    }

    /**
     * Reactivates one account.
     *
     * @param username the account to reactivate
     */
    public void execute(String username) {
        reactivateUser.execute(new ReactivateUserInputData(username));
    }
}
