package use_case.record_attendance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import entity.AttendanceRecord;

/** Authorized coach's existing roster statuses and performance notes. */
public final class RecordAttendanceRosterData {
    private final Map<String, AttendanceRecord.Status> statuses;
    private final Map<String, String> notes;

    public RecordAttendanceRosterData(Map<String, AttendanceRecord.Status> statuses) {
        this(statuses, Map.of());
    }

    public RecordAttendanceRosterData(Map<String, AttendanceRecord.Status> statuses,
            Map<String, String> notes) {
        this.statuses = Collections.unmodifiableMap(new LinkedHashMap<>(statuses));
        this.notes = Collections.unmodifiableMap(new LinkedHashMap<>(notes));
    }

    public Map<String, AttendanceRecord.Status> getStatuses() { return statuses; }
    public Map<String, String> getNotes() { return notes; }
}
