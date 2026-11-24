package use_case.accounts;

/**
 * Thrown by a store when a new account would replace one that already exists.
 *
 * It carries the username and no sentence: the use case that asked for the write
 * decides how to say it, and the three stores can report the same failure without
 * each inventing wording. It replaces the "already exists" messages the data layer
 * used to build, which put user-facing text in the outer layer.
 */
public class DuplicateUsernameException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient String username;

    /**
     * Creates the failure.
     *
     * @param username the username that is already stored
     */
    public DuplicateUsernameException(String username) {
        super("An account with username " + username + " already exists.");
        this.username = username;
    }

    /**
     * The username that is already stored.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }
}
