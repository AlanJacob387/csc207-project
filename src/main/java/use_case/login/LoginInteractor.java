package use_case.login;

import java.io.UncheckedIOException;

import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Log In use case.
 *
 * Checks the entered details against the stored account, records the user as
 * the current user and tells the presenter what happened. Recording the user
 * here (rather than in the presenter) keeps every session transition inside a
 * use case.
 */
public class LoginInteractor implements LoginInputBoundary {

    private final LoginUserDataAccessInterface dataAccess;
    private final LoginOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where accounts are read from
     * @param presenter   where the result is sent
     * @param currentUser the port that records who is signed in
     */
    public LoginInteractor(LoginUserDataAccessInterface dataAccess,
                           LoginOutputBoundary presenter,
                           CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
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
        if (!user.matches(password)) {
            presenter.prepareFailView("Incorrect password.");
            return;
        }
        // The password is checked first, so this message cannot be used to find
        // out which accounts an administrator deactivated.
        if (!user.isActive()) {
            presenter.prepareFailView("This account has been deactivated.");
            return;
        }

        currentUser.setCurrentUser(user);
        presenter.prepareSuccessView(new LoginOutputData(user.getUsername()));
    }
}