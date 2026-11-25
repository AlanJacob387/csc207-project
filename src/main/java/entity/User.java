package entity;

/**
 * A user of the platform. Every user has exactly one role.
 *
 * The stored password is a credential, not necessarily what the user typed:
 * new accounts hold a PBKDF2 hash, older ones may still hold plaintext. Check a
 * typed password with {@link #matches(String)} and change one with
 * {@link #withNewPassword(String)}, so hashing cannot be skipped by accident.
 * An inactive account stays in the store - events, announcements and attendance
 * still refer to it - but it is refused at login.
 */
public class User {
    private final String username;
    private final String password;
    private final String displayName;
    private final Role role;
    private final boolean active;

    /**
     * Creates an active user.
     *
     * @param username    the username
     * @param password    the stored credential (a hash, or legacy plaintext)
     * @param displayName the name to show in the app
     * @param role        the user's role
     */
    public User(String username, String password, String displayName, Role role) {
        this(username, password, displayName, role, true);
    }

    /**
     * Creates a user with an explicit active flag.
     *
     * @param username    the username
     * @param password    the stored credential (a hash, or legacy plaintext)
     * @param displayName the name to show in the app
     * @param role        the user's role
     * @param active      whether the account may be used to log in
     */
    public User(String username, String password, String displayName, Role role, boolean active) {
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.role = role;
        this.active = active;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }
    /**
     * Says whether the account may be used to log in.
     *
     * @return false for an account an administrator deactivated
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Says whether a typed password belongs to this account.
     *
     * @param typedPassword what the user entered
     * @return true only when the stored credential matches
     */
    public boolean matches(String typedPassword) {
        return PasswordHasher.matches(typedPassword, password);
    }

    /**
     * Returns this user with a new, hashed password.
     *
     * The password has to satisfy {@link AccountRules}: the change-password use
     * case asks the rule first so the user sees the sentence, and this refuses as
     * a backstop, so no slice can store a credential the rule would reject.
     *
     * @param newPassword the new plaintext password
     * @return a copy carrying the hash, with the same role and status
     * @throws InvalidAccountException when {@link AccountRules} refuses the password
     */
    public User withNewPassword(String newPassword) {
        final AccountRules.Refusal refusal = AccountRules.passwordRefusalFor(newPassword);
        if (refusal != null) {
            throw new InvalidAccountException(refusal);
        }
        return new User(username, PasswordHasher.hash(newPassword), displayName, role, active);
    }

    /**
     * Returns this user with a different role.
     *
     * @param newRole the role to assign
     * @return a copy with the same credential and status
     */
    public User withRole(Role newRole) {
        return new User(username, password, displayName, newRole, active);
    }

    /**
     * Returns this user with a different active flag.
     *
     * @param newActive whether the account may be used to log in
     * @return a copy with the same credential and role
     */
    public User withActive(boolean newActive) {
        return new User(username, password, displayName, role, newActive);
    }

}