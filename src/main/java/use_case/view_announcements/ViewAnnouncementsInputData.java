package use_case.view_announcements;

/** The signed-in reader and optional team selection. */
public final class ViewAnnouncementsInputData {
    private final String username;
    private final String teamId;

    public ViewAnnouncementsInputData(String username, String teamId) {
        this.username = username;
        this.teamId = teamId;
    }

    public String getUsername() {
        return username;
    }

    public String getTeamId() {
        return teamId;
    }
}
