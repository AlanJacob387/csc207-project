package entity;

import java.time.LocalDateTime;
import java.util.Objects;

/** A coach's message to a team, kept with its original author and posting time. */
public final class Announcement {
    private final String announcementId;
    private final String teamId;
    private final String authorUsername;
    private final String message;
    private final LocalDateTime createdAt;

    /** Creates a complete announcement for posting or restoring from storage. */
    public Announcement(String announcementId, String teamId, String authorUsername,
                        String message, LocalDateTime createdAt) {
        this.announcementId = Objects.requireNonNull(announcementId);
        this.teamId = Objects.requireNonNull(teamId);
        this.authorUsername = Objects.requireNonNull(authorUsername);
        this.message = Objects.requireNonNull(message);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getAnnouncementId() {
        return announcementId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
