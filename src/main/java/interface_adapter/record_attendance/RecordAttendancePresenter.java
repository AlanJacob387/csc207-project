package interface_adapter.record_attendance;
import use_case.record_attendance.RecordAttendanceOutputBoundary;
import use_case.record_attendance.RecordAttendanceOutputData;
import use_case.record_attendance.RecordAttendanceRosterData;
/** Translates use-case results into UI state. */
public final class RecordAttendancePresenter implements RecordAttendanceOutputBoundary {
    private final RecordAttendanceViewModel viewModel;
    public RecordAttendancePresenter(RecordAttendanceViewModel viewModel) {
        this.viewModel = viewModel;
    }
    @Override
    public void prepareSuccessView(RecordAttendanceOutputData output) {
        viewModel.setResult("Saved " + output.getSavedCount() + " attendance records.", true);
    }
    @Override
    public void prepareRosterView(RecordAttendanceRosterData data) {
        viewModel.setRoster(data.getStatuses(), data.getNotes());
    }
    @Override
    public void prepareFailView(String message) {
        viewModel.setResult(message, false);
    }
}
