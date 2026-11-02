package use_case.login;

import entity.User;

/**
 * Data access interface required by the Log In use case.
 * Implemented in the data_access layer (local JSON file, in-memory fake).
 *
 * It declares only what logging in needs: looking an account up. Checking
 * whether a username is taken belongs to Sign Up, so that method lives on
 * SignupUserDataAccessInterface instead.
 */
public interface LoginUserDataAccessInterface {

    /**
     * Looks up a user by username.
     *
     * @param username the username to look up
     * @return the matching user, or null if no such user exists
     */
    User get(String username);
}