package use_case.change_password;

import entity.User;

/**
 * Data access interface required by the Change Password use case.
 * It declares only what this use case needs: read one account, write it back.
 */
public interface ChangePasswordUserDataAccessInterface {

    /**
     * Looks up a user by username.
     *
     * @param username the username to look up
     * @return the matching user, or null if no such user exists
     */
    User get(String username);

    /**
     * Persists a user, replacing the stored one with the same username.
     *
     * @param user the user to save
     */
    void save(User user);
}