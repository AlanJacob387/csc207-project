package use_case.manage_users;

import java.io.UncheckedIOException;

import entity.InvalidAccountException;
import entity.Role;
import entity.UserFactory;
import use_case.accounts.AccountRefusalText;
import use_case.accounts.DuplicateUsernameException;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Create User use case.
 *
 * An administrator creates an account with a chosen role. The password goes
 * through the factory, which hashes it, so the page never writes a plaintext
 * credential. The caller is re-checked here rather than trusted because the
 * sidebar only hides the page.
 */
public class CreateUserInteractor implements CreateUserInputBoundary {

    private final ManageUsersDataAccessInterface dataAccess;
    private final ManageUsersOutputBoundary presenter;
    private final CurrentUserInterface currentUser;
    private final UserFactory userFactory;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where accounts are read and written
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     * @param userFactory builds the new account
     */
    public CreateUserInteractor(ManageUsersDataAccessInterface dataAccess,
                                ManageUsersOutputBoundary presenter,
                                CurrentUserInterface currentUser,
                                UserFactory userFactory) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
        this.userFactory = userFactory;
    }

    @Override
    public void execute(CreateUserInputData inputData) {
        final String refusal = AdminPolicy.refusalFor(currentUser, dataAccess);
        if (refusal != null) {
            presenter.prepareFailView(refusal);
            return;
        }

        final String username = inputData.getUsername() == null
                ? "" : inputData.getUsername().trim();
        final String displayName = inputData.getDisplayName() == null
                ? "" : inputData.getDisplayName().trim();
        final String password = inputData.getPassword() == null ? "" : inputData.getPassword();
        final String repeatPassword = inputData.getRepeatPassword() == null
                ? "" : inputData.getRepeatPassword();

        if (username.isEmpty() || password.isEmpty()) {
            presenter.prepareFailView("Choose a username and a password.");
            return;
        }
        if (!password.equals(repeatPassword)) {
            presenter.prepareFailView("The two passwords do not match.");
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

        try {
            if (dataAccess.existsByName(username)) {
                presenter.prepareFailView("That username is already taken.");
                return;
            }
            dataAccess.createUser(userFactory.create(username, password,
                    displayName.isEmpty() ? username : displayName, role));
            presenter.prepareSuccessView(ManageUsersOutputData.of(
                    "Account created for " + username + ".", dataAccess.getAllUsers()));
        }
        catch (DuplicateUsernameException ex) {
            presenter.prepareFailView("That username is already taken.");
        }
        catch (InvalidAccountException ex) {
            presenter.prepareFailView(AccountRefusalText.forRefusal(ex.getRefusal()));
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the account: " + ex.getMessage());
        }
    }
}
