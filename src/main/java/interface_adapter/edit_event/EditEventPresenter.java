package interface_adapter.edit_event;

import use_case.edit_event.EditEventOutputBoundary;
import use_case.edit_event.EditEventOutputData;

/**
 * Presenter for the Edit Event use case.
 */
public class EditEventPresenter implements EditEventOutputBoundary {
    private final EditEventViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the edit form's view model
     */
    public EditEventPresenter(EditEventViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(EditEventOutputData outputData) {
        viewModel.setSuccessMessage(outputData.getMessage());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setErrorMessage(errorMessage);
    }
}
