package interface_adapter.submit_absence_notice;

import use_case.submit_absence_notice.SubmitAbsenceNoticeOutputBoundary;
import use_case.submit_absence_notice.SubmitAbsenceNoticeOutputData;

/** Converts a use-case outcome into the observed page state. */
public class SubmitAbsenceNoticePresenter implements SubmitAbsenceNoticeOutputBoundary {
    private final SubmitAbsenceNoticeViewModel viewModel;

    public SubmitAbsenceNoticePresenter(SubmitAbsenceNoticeViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(SubmitAbsenceNoticeOutputData data) {
        viewModel.showSuccess(data);
    }

    @Override
    public void prepareFailView(String message) {
        viewModel.showError(message);
    }
}
