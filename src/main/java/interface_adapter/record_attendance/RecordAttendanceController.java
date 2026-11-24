package interface_adapter.record_attendance;
import java.util.Map;
import entity.AttendanceRecord;
import use_case.record_attendance.RecordAttendanceInputBoundary;
import use_case.record_attendance.RecordAttendanceInputData;
/** Converts the coach's form into use-case input. */
public final class RecordAttendanceController {
    private final RecordAttendanceInputBoundary interactor;
    public RecordAttendanceController(RecordAttendanceInputBoundary interactor) {
        this.interactor = interactor;
    }
    public void loadRoster(String eventId) { interactor.loadRoster(eventId); }
    public void execute(String eventId, Map<String, AttendanceRecord.Status> statuses) {
        interactor.execute(new RecordAttendanceInputData(eventId, statuses));
    }
    public void execute(String eventId, Map<String, AttendanceRecord.Status> statuses,
            Map<String, String> notes) {
        interactor.execute(new RecordAttendanceInputData(eventId, statuses, notes));
    }
}
