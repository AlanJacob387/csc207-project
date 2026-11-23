package use_case.create_team;

import entity.Team;
import entity.User;

/** Reads the acting user, checks team IDs, and saves a new team. */
public interface CreateTeamDataAccessInterface {
    User get(String username);

    boolean teamExists(String teamId);

    void saveTeam(Team team);
}
