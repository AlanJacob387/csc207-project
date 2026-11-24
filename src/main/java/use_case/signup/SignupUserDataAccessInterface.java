package use_case.signup;

import use_case.accounts.CreateAccountDataAccessInterface;

/**
 * Data access interface required by the Sign Up use case.
 * Implemented in the data_access layer (local JSON file, hosted database,
 * in-memory fake).
 *
 * Sign up only creates accounts, so the port offers creation plus the username
 * check it shows first; it deliberately cannot read or update an account.
 */
public interface SignupUserDataAccessInterface extends CreateAccountDataAccessInterface {

    /**
     * Checks whether a username is already taken.
     *
     * @param username the username to check
     * @return true if a user with that username already exists
     */
    boolean existsByName(String username);
}
