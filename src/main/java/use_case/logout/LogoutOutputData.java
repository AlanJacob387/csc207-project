package use_case.logout;

/**
 * Output data for the Log Out use case: who was signed out.
 */
public class LogoutOutputData {

    private final String username;

    /**
     * Creates the output data.
     *
     * @param username the user who was signed out
     */
    public LogoutOutputData(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}