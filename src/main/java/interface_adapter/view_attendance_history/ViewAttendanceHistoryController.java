package interface_adapter.view_attendance_history;
import use_case.view_attendance_history.ViewAttendanceHistoryInputBoundary;
import use_case.view_attendance_history.ViewAttendanceHistoryInputData;
public final class ViewAttendanceHistoryController {
 private final ViewAttendanceHistoryInputBoundary interactor;
 public ViewAttendanceHistoryController(ViewAttendanceHistoryInputBoundary interactor) { this.interactor = interactor; }
 public void execute() { interactor.execute(new ViewAttendanceHistoryInputData()); }
}
