package interface_adapter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import entity.Role;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Who is currently signed in.
 *
 * Implements the CurrentUserInterface port, so the use cases own the
 * transitions - the login interactor records the user and the logout
 * interactor clears them - while this class stays plain observable state.
 * The app shell listens for changes so it can rebuild the sidebar for the new
 * role. A null role means nobody is signed in.
 */
public class Session implements CurrentUserInterface {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String username = "";
    private String displayName = "";
    private Role role;

    @Override
    public String getCurrentUsername() {
        return username;
    }

    @Override
    public void setCurrentUser(User user) {
        this.username = user.getUsername();
        this.displayName = user.getDisplayName();
        this.role = user.getRole();
        support.firePropertyChange("session", null, this);
    }

    @Override
    public void clearCurrentUser() {
        this.username = "";
        this.displayName = "";
        this.role = null;
        support.firePropertyChange("session", null, this);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Role getRole() {
        return role;
    }

    /**
     * Says whether anyone is signed in.
     *
     * @return true once a user has logged in
     */
    public boolean isLoggedIn() {
        return role != null;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}