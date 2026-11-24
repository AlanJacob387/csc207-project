package use_case.accounts;

import entity.User;

/**
 * Reads one stored account by username.
 *
 * Narrow on purpose: the authorization check in the account use cases needs to
 * re-read the caller's stored account and nothing else, so the port asks for
 * exactly that instead of a whole administrator interface.
 */
public interface GetAccountDataAccessInterface {

    /**
     * Looks up one account.
     *
     * @param username the username to look up
     * @return the account, or null when there is none
     */
    User get(String username);
}
