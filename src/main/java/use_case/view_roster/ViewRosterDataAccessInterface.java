package use_case.view_roster;

import java.util.List;

import entity.Team;
import entity.User;

/** Reads the viewer, every stored team, and every user (for names and dropdowns). */
public interface ViewRosterDataAccessInterface {
    User get(String username);

    List<Team> getAllTeams();

    List<User> getAllUsers();
}
