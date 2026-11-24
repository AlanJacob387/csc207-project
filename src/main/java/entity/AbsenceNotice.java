package entity;

import java.time.LocalDateTime;
import java.util.Objects;

/** Advance notice from a parent, separate from the coach's attendance record. */
public final class AbsenceNotice {
    private final String noticeId;
    private final String parentUsername;
    private final String playerUsername;
    private final String eventId;
    private final String reason;
    private final LocalDateTime submittedAt;

    public AbsenceNotice(String noticeId, String parentUsername, String playerUsername,
                         String eventId, String reason, LocalDateTime submittedAt) {
        this.noticeId = Objects.requireNonNull(noticeId);
        this.parentUsername = Objects.requireNonNull(parentUsername);
        this.playerUsername = Objects.requireNonNull(playerUsername);
        this.eventId = Objects.requireNonNull(eventId);
        this.reason = Objects.requireNonNull(reason);
        this.submittedAt = Objects.requireNonNull(submittedAt);
    }

    public String getNoticeId() {
        return noticeId;
    }

    public String getParentUsername() {
        return parentUsername;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public String getEventId() {
        return eventId;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
