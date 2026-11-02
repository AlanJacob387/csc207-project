package interface_adapter.change_password;

import use_case.change_password.ChangePasswordInputBoundary;
import use_case.change_password.ChangePasswordInputData;

/**
 * Controller for the Change Password use case.
 * Called by the account page; converts raw input into use-case input data.
 */
public class ChangePasswordController {

    private final ChangePasswordInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the Change Password use case
     */
    public ChangePasswordController(ChangePasswordInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Attempts to change the signed-in user''s password.
     *
     * @param currentPassword   the password in use now
     * @param newPassword       the password to switch to
     * @param repeatNewPassword the new password typed a second time
     */
    public void execute(String currentPassword, String newPassword, String repeatNewPassword) {
        interactor.execute(new ChangePasswordInputData(
                currentPassword, newPassword, repeatNewPassword));
    }
}