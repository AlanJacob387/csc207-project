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

    /**
     * Records a successful create and clears any earlier error, since the view
     * shows the error whenever there is one.
     *
     * @param successMessage message for the user
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
        this.errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Records why the create failed and clears any earlier success message.
     *
     * @param errorMessage message for the user
     */
    public void setErrorMessage(String errorMessage) {
        this.successMessage = "";
        this.errorMessage = errorMessage;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}