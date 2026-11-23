package use_case.current_user;
import entity.User;
/** Interface for the authenticated account, independent of the view. */
public interface CurrentUserInterface {
    String getCurrentUsername();
    void setCurrentUser(User user);
    void clearCurrentUser();
}
