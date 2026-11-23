package interface_adapter.change_password;

import use_case.change_password.ChangePasswordOutputBoundary;
import use_case.change_password.ChangePasswordOutputData;

/**
 * Presenter for the Change Password use case.
 * The account page stays put, so this only updates its view model.
 */
public class ChangePasswordPresenter implements ChangePasswordOutputBoundary {

    private final ChangePasswordViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the account page state
     */
    public ChangePasswordPresenter(ChangePasswordViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ChangePasswordOutputData outputData) {
        viewModel.setSuccessMessage(outputData.getMessage());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setErrorMessage(errorMessage);
    }
}