package use_case.record_attendance;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import entity.AttendanceRecord;
/** Authenticated coach's roster and previous statuses for editing. */
public final class RecordAttendanceRosterData {
 private final Map<String, AttendanceRecord.Status> statuses;
 public RecordAttendanceRosterData(Map<String, AttendanceRecord.Status> statuses) {
  this.statuses = Collections.unmodifiableMap(new LinkedHashMap<>(statuses));
 }
 public Map<String, AttendanceRecord.Status> getStatuses() { return statuses; }
}
