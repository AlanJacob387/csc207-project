package interface_adapter.edit_attendance;
import use_case.edit_attendance.EditAttendanceOutputBoundary;
import use_case.edit_attendance.EditAttendanceOutputData;
public final class EditAttendancePresenter implements EditAttendanceOutputBoundary {
 private final EditAttendanceViewModel vm;
 public EditAttendancePresenter(EditAttendanceViewModel vm) { this.vm = vm; }
 @Override public void prepareSuccessView(EditAttendanceOutputData data) { vm.setResult(data.getMessage(), true); }
 @Override public void prepareFailView(String message) { vm.setResult(message, false); }
}
