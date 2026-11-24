package use_case.signup;

import java.io.UncheckedIOException;

import entity.AccountRules;
import entity.Role;
import entity.User;
import entity.UserFactory;

/**
 * Interactor for the Sign Up use case.
 *
 * Checks the chosen details, refuses a username that is already taken and then
 * stores the new account.
 */
public class SignupInteractor implements SignupInputBoundary {

    private final SignupUserDataAccessInterface dataAccess;
    private final SignupOutputBoundary presenter;
    private final UserFactory userFactory;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where accounts are stored
     * @param presenter   where the result is sent
     * @param userFactory builds the new user entity
     */
    public SignupInteractor(SignupUserDataAccessInterface dataAccess,
                            SignupOutputBoundary presenter,
                            UserFactory userFactory) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.userFactory = userFactory;
    }

    @Override
    public void execute(SignupInputData inputData) {
        final String username = inputData.getUsername() == null
                ? "" : inputData.getUsername().trim();
        final String password = inputData.getPassword() == null ? "" : inputData.getPassword();
        final String repeatPassword = inputData.getRepeatPassword() == null
                ? "" : inputData.getRepeatPassword();
        final String typedName = inputData.getDisplayName() == null
                ? "" : inputData.getDisplayName().trim();

        if (username.isEmpty() || password.isEmpty()) {
            presenter.prepareFailView("Choose a username and a password.");
            return;
        }
        if (!password.equals(repeatPassword)) {
            presenter.prepareFailView("The two passwords do not match.");
            return;
        }

        final String displayName = typedName.isEmpty() ? username : typedName;
        final String refusal = AccountRules.refusalFor(username, displayName, password);
        if (refusal != null) {
            presenter.prepareFailView(refusal);
            return;
        }

        final Role role;
        try {
            role = Role.valueOf(inputData.getRoleName());
        }
        catch (IllegalArgumentException | NullPointerException ex) {
            presenter.prepareFailView("Choose a role from the list.");
            return;
        }
        if (role == Role.ADMIN) {
            // The page does not offer it, so this only happens if something calls
            // the use case directly. The refusal stays in business logic anyway, the
            // same way AdminPolicy does not trust the sidebar.
            presenter.prepareFailView(
                    "Administrator accounts are created by an administrator.");
            return;
        }

        try {
            if (dataAccess.existsByName(username)) {
                presenter.prepareFailView("That username is already taken.");
                return;
            }
            dataAccess.save(userFactory.create(username, password, displayName, role));
        }
        catch (IllegalStateException | IllegalArgumentException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the account: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(new SignupOutputData(username,
                "Account created for " + username + ". Please log in."));
    }
}
