package interface_adapter.create_team;

import use_case.create_team.CreateTeamOutputBoundary;
import use_case.create_team.CreateTeamOutputData;

/** Turns the create team result into a message for the Roster page. */
public class CreateTeamPresenter implements CreateTeamOutputBoundary {
    private final CreateTeamViewModel viewModel;

    public CreateTeamPresenter(CreateTeamViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(CreateTeamOutputData outputData) {
        viewModel.showSuccess("Created " + outputData.getName()
                + " (" + outputData.getTeamId() + ").");
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
