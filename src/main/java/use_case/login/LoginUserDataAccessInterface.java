package use_case.login;

import entity.User;

/**
 * Data access interface required by the Log In use case.
 * Implemented in the data_access layer (local JSON file, in-memory fake).
 *
 * It declares almost nothing: looking an account up, plus the one write logging
 * in needs. A credential stored before hashing existed is rewritten with a hash
 * the first time its owner signs in, so a plaintext password cannot stay in the
 * store - and in a readable table - simply because the account is never reopened.
 * Checking whether a username is taken belongs to Sign Up, so that method lives
 * on SignupUserDataAccessInterface instead.
 */
public interface LoginUserDataAccessInterface {

    /**
     * Looks up a user by username.
     *
     * @param username the username to look up
     * @return the matching user, or null if no such user exists
     */
    User get(String username);

    /**
     * Saves an account, replacing the stored one with the same username.
     *
     * @param user the account to save
     */
    void save(User user);
}