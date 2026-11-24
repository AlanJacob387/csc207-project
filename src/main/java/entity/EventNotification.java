package entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;

/** One durable record of an event being created, changed or cancelled. */
public final class EventNotification {
    public static final String CREATED = "CREATED";
    public static final String CHANGED = "CHANGED";
    public static final String CANCELLED = "CANCELLED";
    private static final Set<String> CHANGE_TYPES = Set.of(CREATED, CHANGED, CANCELLED);

    private final String notificationId;
    private final String eventId;
    private final String teamId;
    private final String changeType;
    private final String message;
    private final LocalDateTime createdAt;

    /** Creates a complete event change record for saving or restoring from storage. */
    public EventNotification(String notificationId, String eventId, String teamId,
                             String changeType, String message, LocalDateTime createdAt) {
        this.notificationId = requireText(notificationId, "Notification id");
        this.eventId = requireText(eventId, "Event id");
        this.teamId = requireText(teamId, "Team id");
        if (!CHANGE_TYPES.contains(Objects.requireNonNull(changeType))) {
            throw new IllegalArgumentException("Unknown event change type: " + changeType);
        }
        this.changeType = changeType;
        this.message = requireText(message, "Message");
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
