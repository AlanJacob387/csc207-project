package use_case.view_attendance_history;
import java.time.LocalDateTime;
import entity.AttendanceRecord;
/** Immutable display data, not an entity crossing the output boundary. */
public final class AttendanceHistoryRow {
 private final String eventId;
 private final LocalDateTime date;
 private final String type;
 private final AttendanceRecord.Status status;
 private final String notes;
 public AttendanceHistoryRow(String eventId, LocalDateTime date, String type,
         AttendanceRecord.Status status, String notes) {
  this.eventId = eventId; this.date = date; this.type = type;
  this.status = status; this.notes = notes;
 }
 public String getEventId() { return eventId; }
 public LocalDateTime getDate() { return date; }
 public String getType() { return type; }
 public AttendanceRecord.Status getStatus() { return status; }
 public String getNotes() { return notes; }
}
