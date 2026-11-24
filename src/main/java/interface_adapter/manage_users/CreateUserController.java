package interface_adapter.manage_users;

import use_case.manage_users.CreateUserInputBoundary;
import use_case.manage_users.CreateUserInputData;

/**
 * Controller for Create User.
 *
 * It builds the input data the interactor needs and decides nothing: whether the
 * caller is an administrator, and whether the details are acceptable, are the use
 * case's business.
 */
public class CreateUserController {

    private final CreateUserInputBoundary createUser;

    /**
     * Creates the controller.
     *
     * @param createUser the Create User use case
     */
    public CreateUserController(CreateUserInputBoundary createUser) {
        this.createUser = createUser;
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
    public void execute(String username, String displayName, String password,
                        String repeatPassword, String roleName) {
        createUser.execute(new CreateUserInputData(username, displayName, password,
                repeatPassword, roleName));
    }
}
