package use_case.view_notifications;

/** An optional team filter; the reader's identity always comes from the session. */
public final class ViewNotificationsInputData {
    private final String teamId;

    public ViewNotificationsInputData(String teamId) {
        this.teamId = teamId;
    }

    public String getTeamId() {
        return teamId;
    }
}
