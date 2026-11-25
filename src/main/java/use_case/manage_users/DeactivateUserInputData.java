package use_case.manage_users;

/**
 * Input for deactivating one account.
 */
public class DeactivateUserInputData {

    private final String username;

    /**
     * Creates the input.
     *
     * @param username the account to deactivate
     */
    public DeactivateUserInputData(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
