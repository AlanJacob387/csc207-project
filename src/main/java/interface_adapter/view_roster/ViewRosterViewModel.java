package interface_adapter.view_roster;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Holds the roster text shown on the Roster page, or why it could not be loaded. */
public class ViewRosterViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String rosterText = "";
    private String errorMessage = "";

    public String getRosterText() {
        return rosterText;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Shows freshly loaded rosters. */
    public void showRoster(String text) {
        rosterText = text;
        errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /** Clears the list and shows why it could not be loaded. */
    public void showError(String message) {
        rosterText = "";
        errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
