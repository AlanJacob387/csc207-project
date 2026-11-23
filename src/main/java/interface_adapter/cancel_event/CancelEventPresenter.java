package interface_adapter.cancel_event;

import use_case.cancel_event.CancelEventOutputBoundary;
import use_case.cancel_event.CancelEventOutputData;

/**
 * Presenter for the Cancel Event use case.
 */
public class CancelEventPresenter implements CancelEventOutputBoundary {
    private final CancelEventViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the cancel action's view model
     */
    public CancelEventPresenter(CancelEventViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(CancelEventOutputData outputData) {
        viewModel.setSuccessMessage(outputData.getMessage());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setErrorMessage(errorMessage);
    }
}
