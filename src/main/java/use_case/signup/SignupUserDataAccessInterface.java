package use_case.signup;

import entity.User;

/**
 * Data access interface required by the Sign Up use case.
 * Implemented in the data_access layer (course API, local JSON file, in-memory fake).
 */
public interface SignupUserDataAccessInterface {

    /**
     * Checks whether a username is already taken.
     *
     * @param username the username to check
     * @return true if a user with that username already exists
     */
    boolean existsByName(String username);

    /**
     * Persists a new user.
     *
     * @param user the user to save
     */
    void save(User user);
}
