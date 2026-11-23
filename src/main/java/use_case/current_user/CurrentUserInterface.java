package use_case.current_user;

import entity.User;

/**
 * Port for "who is currently signed in".
 *
 * The account use cases own the transitions through this port: the login
 * interactor records the user, the logout interactor clears them, and the
 * change-password interactor reads them so a user can only change their own
 * password. The session state in interface_adapter implements it, so no use
 * case depends on a concrete class.
 */
public interface CurrentUserInterface {

    /**
     * Says who is signed in.
     *
     * @return the signed-in username, or an empty string if nobody is signed in
     */
    String getCurrentUsername();

    /**
     * Records the signed-in user.
     *
     * @param user the user who just logged in
     */
    void setCurrentUser(User user);

    /** Clears the signed-in user, for example on logout. */
    void clearCurrentUser();
}