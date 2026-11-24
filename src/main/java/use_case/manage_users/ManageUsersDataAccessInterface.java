package use_case.manage_users;

import java.util.List;

import entity.Role;
import entity.User;
import use_case.accounts.GetAccountDataAccessInterface;

/**
 * Data access required by the Manage Users use cases that read and change
 * existing accounts: the account list, one account, the username check and two
 * updates that each change one field of a stored account.
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
     * Changes the role of a stored account and nothing else.
     *
     * Only the role is written, so a password the account's owner changed after
     * the caller last read it is not put back; a whole-row save would restore the
     * old credential.
     *
     * @param username the account to change
     * @param role     the new role
     * @throws IllegalStateException when no such account is stored any more
     */
    void updateRole(String username, Role role);

    /**
     * Activates or deactivates a stored account and changes nothing else.
     *
     * @param username the account to change
     * @param active   the new status
     * @throws IllegalStateException when no such account is stored any more
     */
    void updateActive(String username, boolean active);
}
