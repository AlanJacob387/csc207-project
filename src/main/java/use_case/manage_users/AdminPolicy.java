package use_case.manage_users;

import java.io.UncheckedIOException;
import java.util.List;

import entity.Role;
import entity.User;
import use_case.accounts.GetAccountDataAccessInterface;
import use_case.current_user.CurrentUserInterface;

/**
 * The authorization rules shared by the Manage Users interactors.
 *
 * They live in the use-case layer, not in the view, because hiding the page in the
 * sidebar is a convenience rather than a permission check: a controller can be
 * called by anything.
 *
 * The session only remembers the role it saw at sign-in, so it can be stale: an
 * administrator who was demoted or deactivated in another client would otherwise
 * keep every administrative action until they logged out. Each call therefore
 * re-reads the caller's stored account, and a store that cannot be read is a
 * refusal rather than a pass.
 */
final class AdminPolicy {

    /** Shown when the caller is not an administrator. */
    static final String NOT_ADMIN = "Only an administrator can manage users.";

    /** Shown when the caller's stored account cannot be read. */
    static final String UNCONFIRMED = "Could not confirm your administrator rights: ";

    /** Shown when an action would remove the last active administrator. */
    static final String LAST_ADMIN = "There must be at least one active administrator.";

    private AdminPolicy() {
    }

    /**
     * Refuses a caller who is not a stored, active administrator.
     *
     * @param currentUser who is signed in
     * @param dataAccess  where the stored accounts are read
     * @return the message to show, or null when the caller may manage users
     */
    static String refusalFor(CurrentUserInterface currentUser,
                             GetAccountDataAccessInterface dataAccess) {
        if (currentUser.getCurrentRole() != Role.ADMIN) {
            return NOT_ADMIN;
        }
        final User actor;
        try {
            actor = dataAccess.get(currentUser.getCurrentUsername());
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            return UNCONFIRMED + ex.getMessage();
        }
        if (actor == null || !actor.isActive() || actor.getRole() != Role.ADMIN) {
            return NOT_ADMIN;
        }
        return null;
    }

    /**
     * Says whether acting on this account would leave no active administrator,
     * locking everyone out of the administrative pages.
     *
     * The caller is itself an active administrator and cannot act on its own
     * account, so this can only happen when two administrators act at the same
     * moment. It is kept as that backstop rather than as a rule the page walks
     * into, because the store is read and written in separate requests.
     *
     * @param accounts the stored accounts
     * @param target   the account being demoted or deactivated
     * @return true when the target is the only active administrator
     */
    static boolean isLastActiveAdmin(List<User> accounts, User target) {
        if (!target.isActive() || target.getRole() != Role.ADMIN) {
            return false;
        }
        int activeAdmins = 0;
        for (User account : accounts) {
            if (account.isActive() && account.getRole() == Role.ADMIN) {
                activeAdmins++;
            }
        }
        return activeAdmins <= 1;
    }
}
