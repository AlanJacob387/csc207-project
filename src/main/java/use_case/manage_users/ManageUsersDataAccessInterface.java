package use_case.manage_users;

import java.util.List;

import entity.User;
import use_case.accounts.GetAccountDataAccessInterface;

/**
 * Data access required by the Manage Users use cases that read and change
 * existing accounts: the account list, one account, the username check and a
 * save that replaces a stored account.
 *
 * Creating an account is a separate capability
 * ({@link CreateUserDataAccessInterface}, and behind it
 * {@code use_case.accounts.CreateAccountDataAccessInterface}), so a use case that
 * only lists or re-roles accounts is not asked to be able to create one.
 */
public interface ManageUsersDataAccessInterface extends GetAccountDataAccessInterface {

    /**
     * Returns every stored account.
     *
     * @return the accounts
     */
    List<User> getAllUsers();

    /**
     * Says whether a username is already taken.
     *
     * @param username the username to check
     * @return true when an account uses that username
     */
    boolean existsByName(String username);

    /**
     * Saves an account, replacing the stored one with the same username.
     *
     * Used for changes to an account that already exists - a new role, a new
     * status - never for creation.
     *
     * @param user the account to save
     */
    void save(User user);
}
