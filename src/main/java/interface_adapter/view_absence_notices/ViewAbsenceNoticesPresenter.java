package interface_adapter.view_absence_notices;

import use_case.view_absence_notices.ViewAbsenceNoticesOutputBoundary;
import use_case.view_absence_notices.ViewAbsenceNoticesOutputData;

/** Converts a use-case outcome into the observed page state. */
public class ViewAbsenceNoticesPresenter implements ViewAbsenceNoticesOutputBoundary {
    private final ViewAbsenceNoticesViewModel viewModel;

    public ViewAbsenceNoticesPresenter(ViewAbsenceNoticesViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ViewAbsenceNoticesOutputData data) {
        viewModel.showSuccess(data);
    }

    @Override
    public void prepareFailView(String message) {
        viewModel.showError(message);
    }
}
