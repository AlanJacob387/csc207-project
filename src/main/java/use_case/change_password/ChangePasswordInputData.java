package use_case.change_password;

/**
 * Input data for the Change Password use case.
 *
 * There is deliberately no username field: the use case takes the signed-in
 * user from the current-user port, so a user cannot change somebody else''s
 * password by typing a different name.
 */
public class ChangePasswordInputData {

    private final String currentPassword;
    private final String newPassword;
    private final String repeatNewPassword;

    /**
     * Creates the input data.
     *
     * @param currentPassword   the password in use now
     * @param newPassword       the password to switch to
     * @param repeatNewPassword the new password typed a second time
     */
    public ChangePasswordInputData(String currentPassword, String newPassword,
                                   String repeatNewPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.repeatNewPassword = repeatNewPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getRepeatNewPassword() {
        return repeatNewPassword;
    }
}