package use_case.edit_attendance;
import java.io.UncheckedIOException;
import entity.*;
import use_case.current_user.CurrentUserInterface;
public final class EditAttendanceInteractor implements EditAttendanceInputBoundary {
 private final EditAttendanceDataAccessInterface dao;
 private final EditAttendanceOutputBoundary presenter;
 private final CurrentUserInterface session;
 public EditAttendanceInteractor(EditAttendanceDataAccessInterface dao,
         EditAttendanceOutputBoundary presenter, CurrentUserInterface session) {
  this.dao = dao; this.presenter = presenter; this.session = session;
 }
 @Override public void execute(EditAttendanceInputData data) {
  if (data == null || data.getEventId() == null || data.getEventId().isBlank()
      || data.getPlayerUsername() == null || data.getPlayerUsername().isBlank()
      || data.getStatus() == null) {
   presenter.prepareFailView("Select an existing event, player and status."); return;
  }
  try {
   User user = dao.get(session.getCurrentUsername());
   Event event = dao.getEvent(data.getEventId());
   Team team = event == null ? null : dao.getTeam(event.getTeamId());
   if (user == null || user.getRole() != Role.COACH || team == null
       || !user.getUsername().equals(team.getCoachUsername())) {
    presenter.prepareFailView("Only this team's coach may edit attendance."); return;
   }
   if (!team.getPlayerUsernames().contains(data.getPlayerUsername())) {
    presenter.prepareFailView("Player is not on this team."); return;
   }
   AttendanceRecord old = dao.getAttendanceRecord(data.getEventId(), data.getPlayerUsername());
   if (old == null) { presenter.prepareFailView("No existing attendance record to edit."); return; }
   AttendanceRecord updated = new AttendanceRecord(data.getEventId(), data.getPlayerUsername(), data.getStatus());
   updated.setStatsNotes(data.getNotes() == null || data.getNotes().isBlank()
           ? old.getStatsNotes() : data.getNotes());
   dao.updateAttendanceRecord(updated);
   presenter.prepareSuccessView(new EditAttendanceOutputData("Attendance updated."));
  }
  catch (IllegalStateException | UncheckedIOException ex) {
   presenter.prepareFailView("Could not update attendance: " + ex.getMessage());
  }
 }
}
