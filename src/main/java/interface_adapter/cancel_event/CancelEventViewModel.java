package interface_adapter.cancel_event;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * View model for cancelling an event from the details dialog.
 * Fires "state" after every attempt, successful or not.
 */
public class CancelEventViewModel {
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
     * Records a successful cancellation.
     *
     * @param message confirmation for the user
     */
    public void setSuccessMessage(String message) {
        this.successMessage = message;
        this.errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /**
     * Records a failed cancellation.
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
