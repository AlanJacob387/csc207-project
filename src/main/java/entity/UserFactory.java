package entity;

/**
 * Builds user entities, so use cases do not construct them directly.
 *
 * The factory is the one path that turns a chosen password into a stored
 * credential: {@link #create} hashes it. Rebuilding a user from a credential
 * already in the store must not go through here - use the copy methods on
 * {@link User} ({@code withRole}, {@code withActive}), which keep the hash as is.
 */
public class UserFactory {

    /**
     * Creates an active user, hashing the chosen password.
     *
     * @param username    the username
     * @param password    the plaintext password the user chose
     * @param displayName the name to show in the app
     * @param role        the user's role
     * @return the new user, carrying a hashed credential
     */
    public User create(String username, String password, String displayName, Role role) {
        return new User(username, PasswordHasher.hash(password), displayName, role, true);
    }
}
