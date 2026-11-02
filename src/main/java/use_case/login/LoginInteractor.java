package use_case.login;

import java.io.UncheckedIOException;

import entity.User;

/**
 * Interactor for the Log In use case.
 *
 * Checks the entered details against the stored account and either hands the
 * user's details to the presenter or reports why the login failed.
 */
public class LoginInteractor implements LoginInputBoundary {

    private final LoginUserDataAccessInterface dataAccess;
    private final LoginOutputBoundary presenter;

    /**
     * Creates the interactor.
     *
     * @param dataAccess where accounts are read from
     * @param presenter  where the result is sent
     */
    public LoginInteractor(LoginUserDataAccessInterface dataAccess,
                           LoginOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(LoginInputData inputData) {
        final String username = inputData.getUsername() == null
                ? "" : inputData.getUsername().trim();
        final String password = inputData.getPassword() == null ? "" : inputData.getPassword();

        if (username.isEmpty() || password.isEmpty()) {
            presenter.prepareFailView("Enter both a username and a password.");
            return;
        }

        final User user;
        try {
            user = dataAccess.get(username);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not read the account store: " + ex.getMessage());
            return;
        }

        if (user == null) {
            presenter.prepareFailView("No account found for " + username + ".");
            return;
        }
        if (!user.getPassword().equals(password)) {
            presenter.prepareFailView("Incorrect password.");
            return;
        }

        presenter.prepareSuccessView(new LoginOutputData(
                user.getUsername(), user.getDisplayName(), user.getRole()));
    }
}
