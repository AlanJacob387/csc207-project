package interface_adapter.view_roster;

import use_case.view_roster.ViewRosterOutputBoundary;
import use_case.view_roster.ViewRosterOutputData;

/** Passes the visible teams and selectable accounts to the Roster page. */
public class ViewRosterPresenter implements ViewRosterOutputBoundary {
    private final ViewRosterViewModel viewModel;

    public ViewRosterPresenter(ViewRosterViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ViewRosterOutputData outputData) {
        viewModel.showRoster(outputData.getTeams(), outputData.getCoaches(), outputData.getPlayers());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
