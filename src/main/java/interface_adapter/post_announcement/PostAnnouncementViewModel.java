package interface_adapter.post_announcement;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/** Holds mutually exclusive posting success and error feedback. */
public class PostAnnouncementViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String successMessage = "";
    private String errorMessage = "";
    private String postedTeamId = "";

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getPostedTeamId() {
        return postedTeamId;
    }

    /** Announces a completed save, clearing any previous validation error. */
    public void showSuccess(String teamId) {
        postedTeamId = teamId;
        successMessage = "Announcement posted.";
        errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /** Reports a failed attempt without representing it as a saved post. */
    public void showError(String message) {
        postedTeamId = "";
        successMessage = "";
        errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    /** Clears feedback when the team or signed-in user changes. */
    public void clear() {
        postedTeamId = "";
        successMessage = "";
        errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
