package use_case.record_attendance;
/** Receives the coach's roster submission. */
public interface RecordAttendanceInputBoundary {
    void execute(RecordAttendanceInputData data);
}
