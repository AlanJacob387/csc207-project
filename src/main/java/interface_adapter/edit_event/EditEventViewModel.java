package interface_adapter.edit_event;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * View model for editing an event in the details dialog.
 * Fires "state" after every attempt, successful or not.
 */
public class EditEventViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String successMessage = "";
    private String errorMessage = "";

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Records a saved edit.
     *
     * @param message confirmation for the user
     */
    public void setSuccessMessage(String message) {
        this.successMessage = message;
        this.errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /**
     * Records a failed edit.
     *
     * @param message what went wrong
     */
    public void setErrorMessage(String message) {
        this.successMessage = "";
        this.errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
