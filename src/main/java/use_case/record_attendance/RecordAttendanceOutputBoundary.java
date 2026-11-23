package use_case.record_attendance;
/** Presents success or failure without a Swing dependency. */
public interface RecordAttendanceOutputBoundary {
    void prepareSuccessView(RecordAttendanceOutputData outputData);
    void prepareRosterView(RecordAttendanceRosterData roster);
    void prepareFailView(String message);
}
