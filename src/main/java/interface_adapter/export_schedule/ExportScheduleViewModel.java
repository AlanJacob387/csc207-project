package interface_adapter.export_schedule;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * View model for the calendar's Export button.
 * Holds the outcome of the last export and fires "state" after every attempt.
 */
public class ExportScheduleViewModel {

    /** How the message should be shown. */
    public enum Status { SUCCESS, INFO, ERROR }

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private Status status = Status.INFO;
    private String message = "";

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Records the outcome of an export.
     *
     * @param newStatus  success, information or error
     * @param newMessage message for the user
     */
    public void setResult(Status newStatus, String newMessage) {
        this.status = newStatus;
        this.message = newMessage;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
