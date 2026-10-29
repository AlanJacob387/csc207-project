package use_case.login;

import entity.User;

/**
 * Data access interface required by the Log In use case.
 * Implemented in the data_access layer (course API, local JSON file, in-memory fake).
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
     * Checks whether a username is already taken.
     *
     * @param username the username to check
     * @return true if a user with that username exists
     */
    boolean existsByName(String username);
}
