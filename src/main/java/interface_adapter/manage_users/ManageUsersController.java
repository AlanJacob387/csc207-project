package interface_adapter.manage_users;

import use_case.manage_users.ChangeUserRoleInputBoundary;
import use_case.manage_users.ChangeUserRoleInputData;
import use_case.manage_users.CreateUserInputBoundary;
import use_case.manage_users.CreateUserInputData;
import use_case.manage_users.DeactivateUserInputBoundary;
import use_case.manage_users.DeactivateUserInputData;
import use_case.manage_users.ListAccountsInputBoundary;
import use_case.manage_users.ReactivateUserInputBoundary;
import use_case.manage_users.ReactivateUserInputData;

/**
 * Controller for the administrator page.
 *
 * The page has three actions, so the controller exposes three methods and builds
 * the matching input data. It decides nothing: every rule lives in an interactor.
 */
public class ManageUsersController {

    private final CreateUserInputBoundary createUser;
    private final ChangeUserRoleInputBoundary changeUserRole;
    private final DeactivateUserInputBoundary deactivateUser;
    private final ReactivateUserInputBoundary reactivateUser;
    private final ListAccountsInputBoundary listAccounts;

    /**
     * Creates the controller.
     *
     * @param createUser     the Create User use case
     * @param changeUserRole the Change User Role use case
     * @param deactivateUser the Deactivate User use case
     * @param reactivateUser the Reactivate User use case
     * @param listAccounts   the read use case that fills the page
     */
    public ManageUsersController(CreateUserInputBoundary createUser,
                                 ChangeUserRoleInputBoundary changeUserRole,
                                 DeactivateUserInputBoundary deactivateUser,
                                 ReactivateUserInputBoundary reactivateUser,
                                 ListAccountsInputBoundary listAccounts) {
        this.createUser = createUser;
        this.changeUserRole = changeUserRole;
        this.deactivateUser = deactivateUser;
        this.reactivateUser = reactivateUser;
        this.listAccounts = listAccounts;
    }

    /** Loads the account list, for example when the page is opened. */
    public void listAccounts() {
        listAccounts.execute();
    }

    /**
     * Creates an account.
     *
     * @param username       the new username
     * @param displayName    the name to show, or empty to reuse the username
     * @param password       the chosen password
     * @param repeatPassword the same password typed again
     * @param roleName       the name of the chosen role
     */
    public void createUser(String username, String displayName, String password,
                           String repeatPassword, String roleName) {
        createUser.execute(new CreateUserInputData(username, displayName, password,
                repeatPassword, roleName));
    }

    /**
     * Changes one account's role.
     *
     * @param username the account to change
     * @param roleName the name of the role to assign
     */
    public void changeUserRole(String username, String roleName) {
        changeUserRole.execute(new ChangeUserRoleInputData(username, roleName));
    }

    /**
     * Deactivates one account.
     *
     * @param username the account to deactivate
     */
    public void deactivateUser(String username) {
        deactivateUser.execute(new DeactivateUserInputData(username));
    }

    /**
     * Reactivates one account.
     *
     * @param username the account to reactivate
     */
    public void reactivateUser(String username) {
        reactivateUser.execute(new ReactivateUserInputData(username));
    }
}
