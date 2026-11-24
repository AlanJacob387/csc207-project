package interface_adapter.view_absence_notices;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import use_case.view_absence_notices.ViewAbsenceNoticesOutputData;

/** Keeps the latest authorized result and clears private data after a failed read. */
public class ViewAbsenceNoticesViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private ViewAbsenceNoticesOutputData data;
    private String errorMessage = "";

    public ViewAbsenceNoticesOutputData getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void showSuccess(ViewAbsenceNoticesOutputData result) {
        data = result;
        errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public void showError(String message) {
        data = null;
        errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    /** Removes the previous user's data and feedback. */
    public void clear() {
        data = null;
        errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
