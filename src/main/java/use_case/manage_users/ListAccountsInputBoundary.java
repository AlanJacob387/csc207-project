package use_case.manage_users;

/**
 * Input boundary for reading the account list.
 *
 * The page needs the list before anything can be changed, so it is its own small
 * use case: it re-checks that the caller is an administrator and returns the same
 * snapshot the actions return.
 */
public interface ListAccountsInputBoundary {

    /** Loads every account for the administrator page. */
    void execute();
}
