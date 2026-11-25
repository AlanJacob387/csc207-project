package use_case.signup;

import java.io.UncheckedIOException;

import entity.InvalidAccountException;
import entity.Role;
import entity.User;
import entity.UserFactory;
import use_case.accounts.AccountRefusalText;
import use_case.accounts.DuplicateUsernameException;

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

        final Role role;
        try {
            role = Role.valueOf(inputData.getRoleName());
        }
        catch (IllegalArgumentException | NullPointerException ex) {
            presenter.prepareFailView("Choose a role from the list.");
            return;
        }
        if (!role.isSelfAssignable()) {
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
            dataAccess.createUser(userFactory.create(username, password, displayName, role));
        }
        catch (DuplicateUsernameException ex) {
            // The store refuses the write itself; the pre-check above is only the
            // fast path, and both report the same thing.
            presenter.prepareFailView("That username is already taken.");
            return;
        }
        catch (InvalidAccountException ex) {
            // The factory applies the rule, so this is the only place the refusal
            // is turned into a sentence.
            presenter.prepareFailView(AccountRefusalText.forRefusal(ex.getRefusal()));
            return;
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the account: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(new SignupOutputData(username,
                "Account created for " + username + ". Please log in."));
    }
}
