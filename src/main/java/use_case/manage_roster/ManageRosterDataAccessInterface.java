package use_case.manage_roster;

import entity.Team;
import entity.User;

/** Reads users and teams, and saves a team after its roster changes. */
public interface ManageRosterDataAccessInterface {
    User get(String username);

    Team getTeam(String teamId);

    void saveTeam(Team team);
}
