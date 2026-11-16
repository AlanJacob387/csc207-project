package use_case.view_roster;

import java.util.List;

import entity.Team;
import entity.User;

/** Reads the viewer and every stored team. */
public interface ViewRosterDataAccessInterface {
    User get(String username);

    List<Team> getAllTeams();
}
