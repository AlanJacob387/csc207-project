package interface_adapter.manage_roster;

import use_case.manage_roster.ManageRosterOutputBoundary;
import use_case.manage_roster.ManageRosterOutputData;

/** Turns the roster change result into a message for the Roster page. */
public class ManageRosterPresenter implements ManageRosterOutputBoundary {
    private final ManageRosterViewModel viewModel;

    public ManageRosterPresenter(ManageRosterViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ManageRosterOutputData outputData) {
        final String change = outputData.isAdded() ? " added to " : " removed from ";
        viewModel.showSuccess(outputData.getPlayerUsername() + change
                + outputData.getTeamId() + ".");
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
