package use_case.manage_users;

import java.util.List;

import entity.Role;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * The authorization rules shared by the Manage Users interactors.
 *
 * They live in the use-case layer, not in the view, because hiding the page in the
 * sidebar is a convenience rather than a permission check: a controller can be
 * called by anything.
 */
final class AdminPolicy {

    /** Shown when the caller is not an administrator. */
    static final String NOT_ADMIN = "Only an administrator can manage users.";

    /** Shown when an action would remove the last active administrator. */
    static final String LAST_ADMIN = "There must be at least one active administrator.";

    private AdminPolicy() {
    }

    /**
     * Refuses a caller who is not an administrator.
     *
     * @param currentUser who is signed in
     * @return the message to show, or null when the caller may manage users
     */
    static String refusalFor(CurrentUserInterface currentUser) {
        return currentUser.getCurrentRole() == Role.ADMIN ? null : NOT_ADMIN;
    }

    /**
     * Says whether acting on this account would leave no active administrator,
     * locking everyone out of the administrative pages.
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
