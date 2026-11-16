package interface_adapter.assign_coach;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Holds the success or error message shown after assigning a coach. */
public class AssignCoachViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String successMessage = "";
    private String errorMessage = "";

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Shows a saved change and clears any earlier error. */
    public void showSuccess(String message) {
        successMessage = message;
        errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /** Shows why the change was not saved. */
    public void showError(String message) {
        successMessage = "";
        errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
