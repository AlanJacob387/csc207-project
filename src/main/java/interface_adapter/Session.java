package interface_adapter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import entity.Role;

/**
 * Who is currently signed in.
 *
 * Created once in {@code Main} and shared by the login presenter (which fills
 * it in) and the app shell (which uses the role to decide which pages to show).
 * A null role means nobody is signed in.
 */
public class Session {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String username = "";
    private String displayName = "";
    private Role role;

    /**
     * Records a successful login.
     *
     * @param username    the user's username
     * @param displayName the user's display name
     * @param role        the user's role
     */
    public void logIn(String username, String displayName, Role role) {
        this.username = username;
        this.displayName = displayName;
        this.role = role;
        support.firePropertyChange("session", null, this);
    }

    /** Clears the session, for example when the user logs out. */
    public void logOut() {
        this.username = "";
        this.displayName = "";
        this.role = null;
        support.firePropertyChange("session", null, this);
    }

    public String getUsername() {
        return username;
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
