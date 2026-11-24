package interface_adapter.link_parent_to_player;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import use_case.link_parent_to_player.LinkParentToPlayerOutputData;

/** Keeps the latest authorized result and clears private data after a failed read. */
public class LinkParentToPlayerViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private LinkParentToPlayerOutputData data;
    private String errorMessage = "";

    public LinkParentToPlayerOutputData getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void showSuccess(LinkParentToPlayerOutputData result) {
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
