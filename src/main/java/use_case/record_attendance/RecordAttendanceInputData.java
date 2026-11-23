package use_case.record_attendance;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import entity.AttendanceRecord;
/** Snapshot of submitted event and player statuses; identity is not supplied by the view. */
public final class RecordAttendanceInputData {
    private final String eventId;
    private final Map<String, AttendanceRecord.Status> statuses;
    private final Map<String, String> notes;

    /** Older callers can submit statuses without changing existing notes. */
    public RecordAttendanceInputData(String eventId, Map<String, AttendanceRecord.Status> statuses) {
        this(eventId, statuses, null);
    }

    /** A complete roster submission including any edited performance notes. */
    public RecordAttendanceInputData(String eventId, Map<String, AttendanceRecord.Status> statuses,
            Map<String, String> notes) {
        this.eventId = eventId;
        this.statuses = statuses == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(statuses));
        this.notes = notes == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(notes));
    }
    public String getEventId() { return eventId; }
    public Map<String, AttendanceRecord.Status> getStatuses() { return statuses; }
    public Map<String, String> getNotes() { return notes; }
}
