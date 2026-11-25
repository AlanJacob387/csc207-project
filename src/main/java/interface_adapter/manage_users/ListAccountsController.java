package interface_adapter.manage_users;

import use_case.manage_users.ListAccountsInputBoundary;

/**
 * Controller for the read that fills the administrator page.
 *
 * It belongs to one use case, like every other controller in the app: the page
 * holds one of these per action instead of a single controller that knows five
 * input boundaries. It decides nothing.
 */
public class ListAccountsController {

    private final ListAccountsInputBoundary listAccounts;

    /**
     * Creates the controller.
     *
     * @param listAccounts the read use case
     */
    public ListAccountsController(ListAccountsInputBoundary listAccounts) {
        this.listAccounts = listAccounts;
    }

    /** Loads the account list, for example when the page is opened. */
    public void execute() {
        listAccounts.execute();
    }
}
