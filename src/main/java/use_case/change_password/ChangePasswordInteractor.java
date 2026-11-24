package use_case.change_password;

import java.io.UncheckedIOException;

import entity.AccountRules;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Change Password use case.
 *
 * The username comes from the current-user port rather than the view, so a
 * signed-in user can only change their own password. The old password must
 * match, the new one must be confirmed, and it must actually be different.
 */
public class ChangePasswordInteractor implements ChangePasswordInputBoundary {

    private final ChangePasswordUserDataAccessInterface dataAccess;
    private final ChangePasswordOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where accounts are read and written
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public ChangePasswordInteractor(ChangePasswordUserDataAccessInterface dataAccess,
                                    ChangePasswordOutputBoundary presenter,
                                    CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(ChangePasswordInputData inputData) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isEmpty()) {
            presenter.prepareFailView("You must be signed in to change your password.");
            return;
        }

        final String currentPassword = inputData.getCurrentPassword() == null
                ? "" : inputData.getCurrentPassword();
        final String newPassword = inputData.getNewPassword() == null
                ? "" : inputData.getNewPassword();
        final String repeatNewPassword = inputData.getRepeatNewPassword() == null
                ? "" : inputData.getRepeatNewPassword();

        if (currentPassword.isEmpty() || newPassword.isEmpty() || repeatNewPassword.isEmpty()) {
            presenter.prepareFailView("Fill in all three password fields.");
            return;
        }
        if (!newPassword.equals(repeatNewPassword)) {
            presenter.prepareFailView("The two new passwords do not match.");
            return;
        }
        if (newPassword.equals(currentPassword)) {
            presenter.prepareFailView("The new password must be different from the current one.");
            return;
        }
        final String refusal = AccountRules.passwordRefusalFor(newPassword);
        if (refusal != null) {
            presenter.prepareFailView(refusal);
            return;
        }

        final User user;
        try {
            user = dataAccess.get(username);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not read the account store: " + ex.getMessage());
            return;
        }
        if (user == null) {
            presenter.prepareFailView("No account found for " + username + ".");
            return;
        }
        if (!user.matches(currentPassword)) {
            presenter.prepareFailView("The current password is incorrect.");
            return;
        }

        try {
            // The entity hashes the new password, so the stored credential is
            // never the plaintext the user typed.
            dataAccess.save(user.withNewPassword(newPassword));
        }
        catch (IllegalStateException | IllegalArgumentException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the new password: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(
                new ChangePasswordOutputData(username, "Password changed."));
    }
}