package use_case.login;

/**
 * Input data for the Log In use case.
 */
public class LoginInputData {

    private final String username;
    private final String password;

    /**
     * Creates the input data.
     *
     * @param username the entered username
     * @param password the entered password
     */
    public LoginInputData(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
