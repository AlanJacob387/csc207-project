package use_case.manage_users;

import java.io.UncheckedIOException;
import java.util.List;

import entity.Role;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Change User Role use case.
 *
 * An administrator changes somebody else's role. Two rules keep the page from
 * locking the team out: an administrator cannot change their own role, and the
 * last active administrator cannot be demoted.
 */
public class ChangeUserRoleInteractor implements ChangeUserRoleInputBoundary {

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
    public ChangeUserRoleInteractor(ManageUsersDataAccessInterface dataAccess,
                                    ManageUsersOutputBoundary presenter,
                                    CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(ChangeUserRoleInputData inputData) {
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

        final Role role;
        try {
            role = Role.valueOf(inputData.getRoleName());
        }
        catch (IllegalArgumentException | NullPointerException ex) {
            presenter.prepareFailView("Choose a role from the list.");
            return;
        }

        try {
            final User target = dataAccess.get(username);
            if (target == null) {
                presenter.prepareFailView("No account found for " + username + ".");
                return;
            }
            if (username.equals(currentUser.getCurrentUsername())) {
                presenter.prepareFailView("You cannot change your own role.");
                return;
            }
            if (target.getRole() == role) {
                presenter.prepareFailView(username + " already has the " + role + " role.");
                return;
            }
            final List<User> accounts = dataAccess.getAllUsers();
            if (role != Role.ADMIN && AdminPolicy.isLastActiveAdmin(accounts, target)) {
                presenter.prepareFailView(AdminPolicy.LAST_ADMIN);
                return;
            }
            dataAccess.save(target.withRole(role));
            presenter.prepareSuccessView(ManageUsersOutputData.of(
                    "Role changed: " + username + " is now " + role + ".",
                    dataAccess.getAllUsers()));
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not update the account: " + ex.getMessage());
        }
    }
}
