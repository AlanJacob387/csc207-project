package entity;

/**
 * Builds user entities, so use cases do not construct them directly.
 */
public class UserFactory {

    /**
     * Creates a user.
     *
     * @param username    the username
     * @param password    the password
     * @param displayName the name to show in the app
     * @param role        the user's role
     * @return the new user
     */
    public User create(String username, String password, String displayName, Role role) {
        return new User(username, password, displayName, role);
    }
}
