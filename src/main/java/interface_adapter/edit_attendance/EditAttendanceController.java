package interface_adapter.edit_attendance;
import use_case.edit_attendance.EditAttendanceInputBoundary;
import use_case.edit_attendance.EditAttendanceInputData;
public final class EditAttendanceController {
 private final EditAttendanceInputBoundary interactor;
 public EditAttendanceController(EditAttendanceInputBoundary interactor) { this.interactor = interactor; }
 public void execute(String eventId, String playerUsername, entity.AttendanceRecord.Status status) {
  execute(eventId, playerUsername, status, null);
 }
 public void execute(String eventId, String playerUsername,
         entity.AttendanceRecord.Status status, String notes) {
  interactor.execute(new EditAttendanceInputData(eventId, playerUsername, status, notes));
 }
}
