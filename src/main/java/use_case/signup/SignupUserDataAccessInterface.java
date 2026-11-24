package use_case.signup;

import entity.User;

/**
 * Data access interface required by the Sign Up use case.
 * Implemented in the data_access layer (local JSON file, in-memory fake).
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
     * Creation is an insert, not a replace: a username that is already stored is
     * reported instead of overwritten, so two people creating the same name at
     * the same moment cannot end up with one of them holding the other's password.
     *
     * @param user the user to create
     */
    void createUser(User user);
}
