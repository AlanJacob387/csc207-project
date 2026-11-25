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

        final User target;
        try {
            target = dataAccess.get(username);
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
            if (role != Role.ADMIN
                    && AdminPolicy.isLastActiveAdmin(dataAccess.getAllUsers(), target)) {
                presenter.prepareFailView(AdminPolicy.LAST_ADMIN);
                return;
            }
            dataAccess.updateRole(username, role);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not update the account: " + ex.getMessage());
            return;
        }

        final String done = "Role changed: " + username + " is now " + role + ".";
        final List<User> accounts;
        try {
            accounts = dataAccess.getAllUsers();
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView(AccountListReport.unrefreshed(done, ex));
            return;
        }
        if (target.getRole() == Role.ADMIN && target.isActive() && role != Role.ADMIN
                && !AdminPolicy.hasActiveAdmin(accounts)) {
            // See DeactivateUserInteractor: the check before the write can be raced, so
            // the write is taken back when it left no active administrator.
            try {
                dataAccess.updateRole(username, Role.ADMIN);
                presenter.prepareFailView(AdminPolicy.LAST_ADMIN);
            }
            catch (IllegalStateException | UncheckedIOException ex) {
                presenter.prepareFailView(AdminPolicy.LAST_ADMIN + " " + username
                        + " is no longer an administrator and could not be restored: "
                        + ex.getMessage());
            }
            return;
        }
        presenter.prepareSuccessView(ManageUsersOutputData.of(done, accounts));
    }
}
