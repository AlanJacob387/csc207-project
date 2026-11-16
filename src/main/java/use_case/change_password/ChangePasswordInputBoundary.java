package use_case.change_password;

/**
 * Input boundary for the Change Password use case.
 */
public interface ChangePasswordInputBoundary {

    /**
     * Changes the signed-in user's password.
     *
     * @param inputData the three passwords typed on the account page
     */
    void execute(ChangePasswordInputData inputData);
}