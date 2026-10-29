package interface_adapter.create_event;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * View model for the Create Event view.
 * The view observes this and re-renders when state changes.
 */
public class CreateEventViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String successMessage = "";
    private String errorMessage = "";

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
        support.firePropertyChange("state", null, this);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}