package use_case.view_roster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.Role;
import entity.Team;
import entity.User;
import use_case.view_roster.ViewRosterOutputData.Person;
import use_case.view_roster.ViewRosterOutputData.TeamRoster;

/**
 * Shows every team to an admin, and only the teams they coach to a coach.
 * Admins also get the coach and player accounts they can pick from.
 */
public class ViewRosterInteractor implements ViewRosterInputBoundary {
    private final ViewRosterDataAccessInterface dataAccess;
    private final ViewRosterOutputBoundary presenter;

    public ViewRosterInteractor(ViewRosterDataAccessInterface dataAccess,
                                ViewRosterOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(ViewRosterInputData inputData) {
        final User user = dataAccess.get(inputData.getUsername());
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.COACH)) {
            presenter.prepareFailView("Only admins and coaches can view rosters.");
            return;
        }
        final boolean isAdmin = user.getRole() == Role.ADMIN;

        final Map<String, User> usersByName = new HashMap<>();
        final List<Person> coaches = new ArrayList<>();
        final List<Person> players = new ArrayList<>();
        for (User account : dataAccess.getAllUsers()) {
            usersByName.put(account.getUsername(), account);
            if (isAdmin && account.getRole() == Role.COACH) {
                coaches.add(toPerson(account.getUsername(), usersByName));
            }
            if (isAdmin && account.getRole() == Role.PLAYER) {
                players.add(toPerson(account.getUsername(), usersByName));
            }
        }

        final List<TeamRoster> visible = new ArrayList<>();
        for (Team team : dataAccess.getAllTeams()) {
            if (isAdmin || user.getUsername().equals(team.getCoachUsername())) {
                final Person coach = team.getCoachUsername().isEmpty()
                        ? null : toPerson(team.getCoachUsername(), usersByName);
                final List<Person> teamPlayers = new ArrayList<>();
                for (String playerUsername : team.getPlayerUsernames()) {
                    teamPlayers.add(toPerson(playerUsername, usersByName));
                }
                visible.add(new TeamRoster(team.getTeamId(), team.getName(), team.getSport(),
                        coach, teamPlayers));
            }
        }

        presenter.prepareSuccessView(new ViewRosterOutputData(visible, coaches, players));
    }

    /** Uses the stored display name, or the username if the account no longer exists. */
    private static Person toPerson(String username, Map<String, User> usersByName) {
        final User account = usersByName.get(username);
        final String displayName = account == null ? username : account.getDisplayName();
        return new Person(username, displayName);
    }
}
