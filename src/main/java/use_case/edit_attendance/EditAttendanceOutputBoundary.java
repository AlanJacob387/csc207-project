package use_case.edit_attendance;
public interface EditAttendanceOutputBoundary {
 void prepareSuccessView(EditAttendanceOutputData data);
 void prepareFailView(String message);
}
