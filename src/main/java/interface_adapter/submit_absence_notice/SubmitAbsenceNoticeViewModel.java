package interface_adapter.submit_absence_notice;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import use_case.submit_absence_notice.SubmitAbsenceNoticeOutputData;

/** Keeps the latest authorized result and clears private data after a failed read. */
public class SubmitAbsenceNoticeViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private SubmitAbsenceNoticeOutputData data;
    private String errorMessage = "";

    public SubmitAbsenceNoticeOutputData getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void showSuccess(SubmitAbsenceNoticeOutputData result) {
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
