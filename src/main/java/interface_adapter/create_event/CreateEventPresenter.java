package interface_adapter.create_event;

import use_case.create_event.CreateEventOutputBoundary;
import use_case.create_event.CreateEventOutputData;

/**
 * Presenter for the Create Event use case.
 * Translates use-case output into view-model state.
 */
public class CreateEventPresenter implements CreateEventOutputBoundary {
    private final CreateEventViewModel viewModel;

    public CreateEventPresenter(CreateEventViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(CreateEventOutputData outputData) {
        viewModel.setSuccessMessage(outputData.getMessage());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setErrorMessage(errorMessage);
    }
}