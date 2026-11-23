package use_case.manage_users;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import entity.Role;
import entity.User;

/**
 * What the administrator page shows after an action: a message plus the current
 * accounts, so the page can redraw itself from one consistent snapshot.
 *
 * Only the fields the page displays are exposed, which keeps the view away from
 * the stored credential.
 */
public final class ManageUsersOutputData {

    private final String message;
    private final List<AccountData> accounts;

    /**
     * Creates the snapshot.
     *
     * @param message  what to show the administrator
     * @param accounts the rows to display
     */
    public ManageUsersOutputData(String message, List<AccountData> accounts) {
        this.message = message;
        this.accounts = List.copyOf(accounts);
    }

    /**
     * Builds a snapshot from the stored accounts.
     *
     * The rows are sorted by username so the list does not jump around after every
     * action.
     *
     * @param message what to show the administrator
     * @param users   the stored accounts
     * @return the snapshot
     */
    public static ManageUsersOutputData of(String message, List<User> users) {
        final List<AccountData> accounts = new ArrayList<>();
        for (User user : users) {
            accounts.add(new AccountData(user));
        }
        accounts.sort(Comparator.comparing(AccountData::getUsername));
        return new ManageUsersOutputData(message, accounts);
    }

    public String getMessage() {
        return message;
    }

    public List<AccountData> getAccounts() {
        return accounts;
    }

    /** One row of the administrator page: enough to identify and act on an account. */
    public static final class AccountData {

        private final String username;
        private final String displayName;
        private final Role role;
        private final boolean active;

        /**
         * Copies the visible fields of an account.
         *
         * @param user the stored account
         */
        public AccountData(User user) {
            this.username = user.getUsername();
            this.displayName = user.getDisplayName();
            this.role = user.getRole();
            this.active = user.isActive();
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Role getRole() {
            return role;
        }

        public boolean isActive() {
            return active;
        }

        /**
         * Describes the row in one line, for the page's account list.
         *
         * @return the username, display name, role and status
         */
        public String describe() {
            return username + " - " + displayName + " - " + role
                    + (active ? " - active" : " - deactivated");
        }
    }
}
