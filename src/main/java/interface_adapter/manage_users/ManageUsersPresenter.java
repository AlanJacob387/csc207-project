package interface_adapter.manage_users;

import use_case.manage_users.ManageUsersOutputBoundary;
import use_case.manage_users.ManageUsersOutputData;

/**
 * Presenter for the administrator page.
 *
 * Every action reports back through the same output boundary, so one presenter
 * serves create, change-role and deactivate.
 */
public class ManageUsersPresenter implements ManageUsersOutputBoundary {

    private final ManageUsersViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the page state to update
     */
    public ManageUsersPresenter(ManageUsersViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ManageUsersOutputData outputData) {
        viewModel.showAccounts(outputData);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
