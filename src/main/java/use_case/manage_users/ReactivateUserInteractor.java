package use_case.manage_users;

import java.io.UncheckedIOException;

import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Reactivate User use case.
 *
 * Deactivating has to be reversible: the row stays so the username cannot be
 * reused, and without this a mistaken click could only be undone in the database.
 * Reactivating flips the flag back and leaves the credential, role and display
 * name untouched.
 */
public class ReactivateUserInteractor implements ReactivateUserInputBoundary {

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
    public ReactivateUserInteractor(ManageUsersDataAccessInterface dataAccess,
                                    ManageUsersOutputBoundary presenter,
                                    CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(ReactivateUserInputData inputData) {
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
            if (target.isActive()) {
                presenter.prepareFailView(username + " is already active.");
                return;
            }
            dataAccess.save(target.withActive(true));
            presenter.prepareSuccessView(ManageUsersOutputData.of(
                    "Account reactivated: " + username + ".", dataAccess.getAllUsers()));
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not update the account: " + ex.getMessage());
        }
    }
}
