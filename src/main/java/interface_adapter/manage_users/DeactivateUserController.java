package interface_adapter.manage_users;

import use_case.manage_users.DeactivateUserInputBoundary;
import use_case.manage_users.DeactivateUserInputData;

/**
 * Controller for Deactivate User. It builds the input data and decides nothing.
 */
public class DeactivateUserController {

    private final DeactivateUserInputBoundary deactivateUser;

    /**
     * Creates the controller.
     *
     * @param deactivateUser the Deactivate User use case
     */
    public DeactivateUserController(DeactivateUserInputBoundary deactivateUser) {
        this.deactivateUser = deactivateUser;
    }

    /**
     * Deactivates one account.
     *
     * @param username the account to deactivate
     */
    public void execute(String username) {
        deactivateUser.execute(new DeactivateUserInputData(username));
    }
}
