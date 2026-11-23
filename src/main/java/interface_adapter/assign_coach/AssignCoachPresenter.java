package interface_adapter.assign_coach;

import use_case.assign_coach.AssignCoachOutputBoundary;
import use_case.assign_coach.AssignCoachOutputData;

/** Turns the assign coach result into a message for the Roster page. */
public class AssignCoachPresenter implements AssignCoachOutputBoundary {
    private final AssignCoachViewModel viewModel;

    public AssignCoachPresenter(AssignCoachViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(AssignCoachOutputData outputData) {
        viewModel.showSuccess(outputData.getCoachUsername() + " now coaches "
                + outputData.getTeamId() + ".");
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
