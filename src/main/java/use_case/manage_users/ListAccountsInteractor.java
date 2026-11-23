package use_case.manage_users;

import java.io.UncheckedIOException;

import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for reading the account list.
 *
 * It exists so the page can be filled before anything is changed, and it is
 * guarded like the other three: the account list is exactly the data an
 * administrator should not be able to read without being one.
 */
public class ListAccountsInteractor implements ListAccountsInputBoundary {

    private final ManageUsersDataAccessInterface dataAccess;
    private final ManageUsersOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where accounts are read
     * @param presenter   where the snapshot is sent
     * @param currentUser the port that says who is signed in
     */
    public ListAccountsInteractor(ManageUsersDataAccessInterface dataAccess,
                                  ManageUsersOutputBoundary presenter,
                                  CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute() {
        final String refusal = AdminPolicy.refusalFor(currentUser);
        if (refusal != null) {
            presenter.prepareFailView(refusal);
            return;
        }
        try {
            presenter.prepareSuccessView(ManageUsersOutputData.of("",
                    dataAccess.getAllUsers()));
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not read the accounts: " + ex.getMessage());
        }
    }
}
