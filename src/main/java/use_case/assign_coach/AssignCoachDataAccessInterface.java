package use_case.assign_coach;

import entity.Team;
import entity.User;

/** Reads users and teams, and saves a team after its coach changes. */
public interface AssignCoachDataAccessInterface {
    User get(String username);

    Team getTeam(String teamId);

    void saveTeam(Team team);
}
