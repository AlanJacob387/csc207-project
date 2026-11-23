package interface_adapter;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * Holds the name of the view that should currently be showing.
 *
 * Presenters set the state and fire a change; the {@code ViewManager} listens
 * and swaps the visible page, so a use case can trigger navigation without the
 * view knowing anything about it.
 */
public class ViewManagerModel {

    /** Page names used across the app. */
    public static final String LOGIN = "login";
    public static final String SIGNUP = "signup";
    public static final String DATABASE = "database";
    public static final String APP = "app";

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String state = LOGIN;

    public String getState() {
        return state;
    }

    /**
     * Sets the view that should be shown.
     *
     * @param state one of the page-name constants on this class
     */
    public void setState(String state) {
        this.state = state;
    }

    /** Tells listeners that the active view changed. */
    public void firePropertyChanged() {
        support.firePropertyChange("state", null, this.state);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
