package use_case.post_announcement;

/** The signed-in author, selected team, and entered message. */
public final class PostAnnouncementInputData {
    private final String username;
    private final String teamId;
    private final String message;

    public PostAnnouncementInputData(String username, String teamId, String message) {
        this.username = username;
        this.teamId = teamId;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getMessage() {
        return message;
    }
}
