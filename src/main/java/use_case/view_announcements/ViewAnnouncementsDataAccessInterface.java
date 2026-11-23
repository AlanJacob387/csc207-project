package use_case.view_announcements;

import java.util.List;

import entity.Announcement;
import entity.Team;
import entity.User;

/** Reads the reader's permissions, available teams, and a team's posts. */
public interface ViewAnnouncementsDataAccessInterface {
    User get(String username);

    List<Team> getAllTeams();

    List<Announcement> getAnnouncementsForTeam(String teamId);

    /** Older stores grant no parent access until an explicit link is available. */
    default List<String> getLinkedPlayerUsernames(String parentUsername) {
        return List.of();
    }
}
