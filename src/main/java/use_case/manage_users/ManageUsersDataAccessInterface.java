package use_case.manage_users;

import java.util.List;

import entity.User;

/**
 * Data access required by the Manage Users use cases.
 *
 * Reading the whole account list is what separates administration from the other
 * account slices, so that method lives here and not in login or signup.
 */
public interface ManageUsersDataAccessInterface {

    /**
     * Returns every stored account.
     *
     * @return the accounts
     */
    List<User> getAllUsers();

    /**
     * Looks up one account.
     *
     * @param username the username to look up
     * @return the account, or null when there is none
     */
    User get(String username);

    /**
     * Says whether a username is already taken.
     *
     * @param username the username to check
     * @return true when an account uses that username
     */
    boolean existsByName(String username);

    /**
     * Stores a new account.
     *
     * An insert, so an existing username is reported rather than replaced: the
     * check in the interactor and the write are separate requests, and this makes
     * the write itself refuse to overwrite somebody else's credential.
     *
     * @param user the account to create
     */
    void createUser(User user);

    /**
     * Saves an account, replacing the stored one with the same username.
     *
     * Used for changes to an account that already exists - a new role, a new
     * status, a new password - never for creation.
     *
     * @param user the account to save
     */
    void save(User user);
}
