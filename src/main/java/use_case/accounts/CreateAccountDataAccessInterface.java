package use_case.accounts;

import entity.User;

/**
 * Stores a new account, and only that.
 *
 * It is its own port so a use case that creates accounts does not have to be able
 * to update them, and so a use case that lists or changes accounts does not have
 * to be able to create one. Both sign up and the administrator's Create User need
 * this one capability, which is why it is shared here rather than declared twice.
 */
public interface CreateAccountDataAccessInterface {

    /**
     * Stores a new account, refusing a username that is already taken.
     *
     * @param user the account to create
     * @throws DuplicateUsernameException when that username is already stored
     */
    void createUser(User user);
}
