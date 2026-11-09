package use_case.view_announcements;

import java.time.LocalDateTime;
import java.util.List;

/** A snapshot of the permitted teams and selected team's posts for presentation. */
public final class ViewAnnouncementsOutputData {
    private final List<TeamData> teams;
    private final String selectedTeamId;
    private final List<AnnouncementData> announcements;
    private final boolean canPost;

    public ViewAnnouncementsOutputData(List<TeamData> teams, String selectedTeamId,
                                      List<AnnouncementData> announcements, boolean canPost) {
        this.teams = List.copyOf(teams);
        this.selectedTeamId = selectedTeamId;
        this.announcements = List.copyOf(announcements);
        this.canPost = canPost;
    }

    public List<TeamData> getTeams() {
        return teams;
    }

    public String getSelectedTeamId() {
        return selectedTeamId;
    }

    public List<AnnouncementData> getAnnouncements() {
        return announcements;
    }

    public boolean canPost() {
        return canPost;
    }

    /** Exposes a selectable team's identity without mutable roster information. */
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

    /** Exposes the fields needed to display a post. */
    public static final class AnnouncementData {
        private final String announcementId;
        private final String authorUsername;
        private final String message;
        private final LocalDateTime createdAt;

        public AnnouncementData(String announcementId, String authorUsername,
                                String message, LocalDateTime createdAt) {
            this.announcementId = announcementId;
            this.authorUsername = authorUsername;
            this.message = message;
            this.createdAt = createdAt;
        }

        public String getAnnouncementId() {
            return announcementId;
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
}
