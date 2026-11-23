package use_case.view_attendance_history;
public interface ViewAttendanceHistoryOutputBoundary {
 void prepareSuccessView(ViewAttendanceHistoryOutputData data);
 void prepareFailView(String message);
}
