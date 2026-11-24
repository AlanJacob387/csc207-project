package interface_adapter.view_notifications;

import use_case.view_notifications.ViewNotificationsOutputBoundary;
import use_case.view_notifications.ViewNotificationsOutputData;

/** Replaces the visible feed with the authorized result of a read. */
public class ViewNotificationsPresenter implements ViewNotificationsOutputBoundary {
    private final ViewNotificationsViewModel viewModel;

    public ViewNotificationsPresenter(ViewNotificationsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ViewNotificationsOutputData outputData) {
        viewModel.showNotifications(outputData);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
