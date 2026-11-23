package use_case.post_announcement;

import entity.Announcement;
import entity.Team;
import entity.User;

/** Reads author/team permissions and durably saves an announcement. */
public interface PostAnnouncementDataAccessInterface {
    User get(String username);

    Team getTeam(String teamId);

    void saveAnnouncement(Announcement announcement);
}
