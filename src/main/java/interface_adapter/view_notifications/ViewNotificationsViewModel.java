package interface_adapter.view_notifications;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import use_case.view_notifications.ViewNotificationsOutputData;
import use_case.view_notifications.ViewNotificationsOutputData.NotificationData;
import use_case.view_notifications.ViewNotificationsOutputData.TeamData;

/** Holds an authorized snapshot and removes old data after errors or session changes. */
public class ViewNotificationsViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private List<TeamData> teams = List.of();
    private List<NotificationData> notifications = List.of();
    private String selectedTeamId = "";
    private String errorMessage = "";
    private boolean loaded;

    public List<TeamData> getTeams() {
        return teams;
    }

    public List<NotificationData> getNotifications() {
        return notifications;
    }

    public String getSelectedTeamId() {
        return selectedTeamId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void showNotifications(ViewNotificationsOutputData outputData) {
        teams = List.copyOf(outputData.getTeams());
        notifications = List.copyOf(outputData.getNotifications());
        selectedTeamId = outputData.getSelectedTeamId();
        errorMessage = "";
        loaded = true;
        support.firePropertyChange("state", null, this);
    }

    public void showError(String message) {
        clearState();
        errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    /** Clears teams, filters and notices before a different account uses the page. */
    public void clear() {
        clearState();
        support.firePropertyChange("state", null, this);
    }

    private void clearState() {
        teams = List.of();
        notifications = List.of();
        selectedTeamId = "";
        errorMessage = "";
        loaded = false;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
