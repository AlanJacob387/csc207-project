package use_case.manage_users;

/**
 * Input for reactivating an account.
 */
public class ReactivateUserInputData {

    private final String username;

    /**
     * Creates the input.
     *
     * @param username the account to reactivate
     */
    public ReactivateUserInputData(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
