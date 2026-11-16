package interface_adapter.view_announcements;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import use_case.view_announcements.ViewAnnouncementsOutputData;
import use_case.view_announcements.ViewAnnouncementsOutputData.AnnouncementData;
import use_case.view_announcements.ViewAnnouncementsOutputData.TeamData;

/** Holds an authorized snapshot; failed loads clear any previously displayed data. */
public class ViewAnnouncementsViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private List<TeamData> teams = List.of();
    private List<AnnouncementData> announcements = List.of();
    private String selectedTeamId = "";
    private String errorMessage = "";
    private boolean canPost;

    public List<TeamData> getTeams() {
        return teams;
    }

    public List<AnnouncementData> getAnnouncements() {
        return announcements;
    }

    public String getSelectedTeamId() {
        return selectedTeamId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean canPost() {
        return canPost;
    }

    /** Replaces the visible snapshot after a successful authorized read. */
    public void showAnnouncements(ViewAnnouncementsOutputData data) {
        teams = List.copyOf(data.getTeams());
        announcements = List.copyOf(data.getAnnouncements());
        selectedTeamId = data.getSelectedTeamId();
        canPost = data.canPost();
        errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /** Clears stale content before reporting a load or authorization failure. */
    public void showError(String message) {
        clearState();
        errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    /** Removes the previous user's teams and posts on a session change. */
    public void clear() {
        clearState();
        support.firePropertyChange("state", null, this);
    }

    private void clearState() {
        teams = List.of();
        announcements = List.of();
        selectedTeamId = "";
        canPost = false;
        errorMessage = "";
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
