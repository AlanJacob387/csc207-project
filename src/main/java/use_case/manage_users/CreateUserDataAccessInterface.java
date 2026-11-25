package use_case.manage_users;

import java.util.List;

import entity.User;
import use_case.accounts.CreateAccountDataAccessInterface;
import use_case.accounts.GetAccountDataAccessInterface;

/**
 * Data access required by the Create User use case.
 *
 * It can confirm the caller (a stored account has to be read before an
 * administrator may act), check the username it is about to use, insert the new
 * account, and hand back the refreshed list the page shows afterwards. It
 * deliberately cannot replace an existing account: creating is not saving, and
 * this slice must not be able to overwrite anybody's credential.
 */
public interface CreateUserDataAccessInterface
        extends GetAccountDataAccessInterface, CreateAccountDataAccessInterface {

    /**
     * Says whether a username is already taken.
     *
     * @param username the username to check
     * @return true when an account uses that username
     */
    boolean existsByName(String username);

    /**
     * Returns every stored account, for the list the page shows after a create.
     *
     * @return the accounts
     */
    List<User> getAllUsers();
}
