package use_case.view_notifications;

import java.time.LocalDateTime;
import java.util.List;

/** An immutable snapshot of permitted teams and their event changes. */
public final class ViewNotificationsOutputData {
    private final List<TeamData> teams;
    private final String selectedTeamId;
    private final List<NotificationData> notifications;

    public ViewNotificationsOutputData(List<TeamData> teams, String selectedTeamId,
                                       List<NotificationData> notifications) {
        this.teams = List.copyOf(teams);
        this.selectedTeamId = selectedTeamId;
        this.notifications = List.copyOf(notifications);
    }

    public List<TeamData> getTeams() {
        return teams;
    }

    public String getSelectedTeamId() {
        return selectedTeamId;
    }

    public List<NotificationData> getNotifications() {
        return notifications;
    }

    /** A team filter choice without mutable roster details. */
    public static final class TeamData {
        private final String teamId;
        private final String name;

        public TeamData(String teamId, String name) {
            this.teamId = teamId;
            this.name = name;
        }

        public String getTeamId() {
            return teamId;
        }

        public String getName() {
            return name;
        }
    }

    /** Display fields captured when an event was changed, rather than reconstructed history. */
    public static final class NotificationData {
        private final String notificationId;
        private final String eventId;
        private final String teamId;
        private final String teamName;
        private final String changeType;
        private final String message;
        private final LocalDateTime createdAt;

        public NotificationData(String notificationId, String eventId, String teamId, String teamName,
                                String changeType, String message, LocalDateTime createdAt) {
            this.notificationId = notificationId;
            this.eventId = eventId;
            this.teamId = teamId;
            this.teamName = teamName;
            this.changeType = changeType;
            this.message = message;
            this.createdAt = createdAt;
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

        public String getTeamName() {
            return teamName;
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
}
