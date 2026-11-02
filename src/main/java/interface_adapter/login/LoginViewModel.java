package interface_adapter.login;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * View model for the Log In view.
 * The view observes this and shows whatever message is current.
 */
public class LoginViewModel {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String errorMessage = "";
    private String successMessage = "";

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Shows an error and clears any success message.
     *
     * @param errorMessage the message to display
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.successMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    /**
     * Shows a success message and clears any error, for example after signing up.
     *
     * @param successMessage the message to display
     */
    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
        this.errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /** Clears both messages, for example after a successful login. */
    public void clear() {
        this.errorMessage = "";
        this.successMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
