package interface_adapter.signup;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * View model for the Sign Up view.
 * The view observes this and shows whatever error is current.
 */
public class SignupViewModel {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String errorMessage = "";

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Shows an error.
     *
     * @param errorMessage the message to display
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        support.firePropertyChange("state", null, this);
    }

    /** Clears the error, for example when the page is opened again. */
    public void clear() {
        this.errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
