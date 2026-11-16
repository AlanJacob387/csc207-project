package interface_adapter.view_roster;

import use_case.view_roster.ViewRosterOutputBoundary;
import use_case.view_roster.ViewRosterOutputData;
import use_case.view_roster.ViewRosterOutputData.TeamRoster;

/** Formats the visible teams as readable text for the Roster page. */
public class ViewRosterPresenter implements ViewRosterOutputBoundary {
    private final ViewRosterViewModel viewModel;

    public ViewRosterPresenter(ViewRosterViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ViewRosterOutputData outputData) {
        if (outputData.getTeams().isEmpty()) {
            viewModel.showRoster("No teams yet.");
            return;
        }

        final StringBuilder text = new StringBuilder();
        for (TeamRoster team : outputData.getTeams()) {
            final String coach = team.getCoachUsername().isEmpty()
                    ? "(none)" : team.getCoachUsername();
            final String players = team.getPlayerUsernames().isEmpty()
                    ? "(none)" : String.join(", ", team.getPlayerUsernames());
            text.append(team.getName()).append(" (").append(team.getTeamId()).append(") - ")
                    .append(team.getSport()).append('\n')
                    .append("    Coach: ").append(coach).append('\n')
                    .append("    Players: ").append(players).append("\n\n");
        }
        viewModel.showRoster(text.toString().trim());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
