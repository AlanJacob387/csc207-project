package use_case.post_announcement;

/** Identifies the saved post and the team whose list should be refreshed. */
public final class PostAnnouncementOutputData {
    private final String announcementId;
    private final String teamId;

    public PostAnnouncementOutputData(String announcementId, String teamId) {
        this.announcementId = announcementId;
        this.teamId = teamId;
    }

    public String getAnnouncementId() {
        return announcementId;
    }

    public String getTeamId() {
        return teamId;
    }
}
