package use_case.view_roster;

import java.util.ArrayList;
import java.util.List;

import entity.Role;
import entity.Team;
import entity.User;
import use_case.view_roster.ViewRosterOutputData.TeamRoster;

/** Shows every team to an admin, and only the teams they coach to a coach. */
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

        final List<TeamRoster> visible = new ArrayList<>();
        for (Team team : dataAccess.getAllTeams()) {
            if (user.getRole() == Role.ADMIN
                    || user.getUsername().equals(team.getCoachUsername())) {
                visible.add(new TeamRoster(team.getTeamId(), team.getName(), team.getSport(),
                        team.getCoachUsername(), team.getPlayerUsernames()));
            }
        }

        presenter.prepareSuccessView(new ViewRosterOutputData(visible));
    }
}
