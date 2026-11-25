package use_case.login;

import java.io.UncheckedIOException;

import entity.AccountRules;
import entity.InvalidAccountException;
import entity.PasswordHasher;
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

        currentUser.setCurrentUser(upgradeLegacyCredential(user, password));
        presenter.prepareSuccessView(new LoginOutputData(user.getUsername()));
    }

    /**
     * Rewrites a credential that was stored before passwords were hashed, now
     * that the typed password has proved it is the right one.
     *
     * A password that the current rule would refuse - an old account whose
     * password is shorter than the minimum, say - is left exactly as it is:
     * rewriting it would either fail or impose today's rule on a sign-in that
     * should keep working, and the owner will meet the rule when they next change
     * it.
     *
     * The rewrite is best effort: a store that cannot be written keeps the old
     * credential, which still verifies next time, so a sign-in never fails
     * because an upgrade could not be saved.
     *
     * @param user     the account that just signed in
     * @param password the password that matched
     * @return the account to record as the current user
     */
    private User upgradeLegacyCredential(User user, String password) {
        if (PasswordHasher.isHashed(user.getPassword())
                || AccountRules.passwordRefusalFor(password) != null) {
            return user;
        }
        try {
            final User hashed = user.withNewPassword(password);
            dataAccess.save(hashed);
            return hashed;
        }
        catch (InvalidAccountException | IllegalStateException | UncheckedIOException ex) {
            // Signing in succeeded; the plaintext credential simply survives until
            // the next attempt. Nothing is shown, because nothing is broken from
            // the user's point of view.
            return user;
        }
    }
}