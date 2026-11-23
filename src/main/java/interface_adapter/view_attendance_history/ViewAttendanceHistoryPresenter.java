package interface_adapter.view_attendance_history;
import use_case.view_attendance_history.*;
public final class ViewAttendanceHistoryPresenter implements ViewAttendanceHistoryOutputBoundary {
 private final ViewAttendanceHistoryViewModel vm;
 public ViewAttendanceHistoryPresenter(ViewAttendanceHistoryViewModel vm) { this.vm = vm; }
 @Override public void prepareSuccessView(ViewAttendanceHistoryOutputData data) { vm.setRows(data.getRows()); }
 @Override public void prepareFailView(String message) { vm.setError(message); }
}
