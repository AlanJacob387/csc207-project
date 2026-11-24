package use_case.manage_users;

import java.io.UncheckedIOException;

import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Deactivate User use case.
 *
 * The account is kept rather than deleted, because events, announcements and
 * attendance still refer to it; login refuses it instead. An administrator cannot
 * deactivate themselves or the last active administrator, so the page can never
 * lock the team out.
 */
public class DeactivateUserInteractor implements DeactivateUserInputBoundary {

    private final ManageUsersDataAccessInterface dataAccess;
    private final ManageUsersOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where accounts are read and written
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public DeactivateUserInteractor(ManageUsersDataAccessInterface dataAccess,
                                    ManageUsersOutputBoundary presenter,
                                    CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(DeactivateUserInputData inputData) {
        final String refusal = AdminPolicy.refusalFor(currentUser, dataAccess);
        if (refusal != null) {
            presenter.prepareFailView(refusal);
            return;
        }

        final String username = inputData.getUsername() == null
                ? "" : inputData.getUsername().trim();
        if (username.isEmpty()) {
            presenter.prepareFailView("Choose an account from the list.");
            return;
        }

        try {
            final User target = dataAccess.get(username);
            if (target == null) {
                presenter.prepareFailView("No account found for " + username + ".");
                return;
            }
            if (username.equals(currentUser.getCurrentUsername())) {
                presenter.prepareFailView("You cannot deactivate your own account.");
                return;
            }
            if (!target.isActive()) {
                presenter.prepareFailView(username + " is already deactivated.");
                return;
            }
            if (AdminPolicy.isLastActiveAdmin(dataAccess.getAllUsers(), target)) {
                presenter.prepareFailView(AdminPolicy.LAST_ADMIN);
                return;
            }
            dataAccess.save(target.withActive(false));
            presenter.prepareSuccessView(ManageUsersOutputData.of(
                    "Account deactivated: " + username + ".", dataAccess.getAllUsers()));
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not update the account: " + ex.getMessage());
        }
    }
}
