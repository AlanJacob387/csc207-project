package use_case.edit_attendance;
import entity.AttendanceRecord;
public final class EditAttendanceInputData {
 private final String eventId;
 private final String playerUsername;
 private final AttendanceRecord.Status status;
 private final String notes;
 public EditAttendanceInputData(String eventId, String playerUsername, AttendanceRecord.Status status) {
  this(eventId, playerUsername, status, null);
 }
 public EditAttendanceInputData(String eventId, String playerUsername,
         AttendanceRecord.Status status, String notes) {
  this.eventId = eventId; this.playerUsername = playerUsername;
  this.status = status; this.notes = notes;
 }
 public String getEventId() { return eventId; }
 public String getPlayerUsername() { return playerUsername; }
 public AttendanceRecord.Status getStatus() { return status; }
 public String getNotes() { return notes; }
}
