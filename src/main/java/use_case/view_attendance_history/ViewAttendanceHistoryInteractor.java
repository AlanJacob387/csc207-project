package use_case.view_attendance_history;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import entity.*;
import use_case.current_user.CurrentUserInterface;
public final class ViewAttendanceHistoryInteractor implements ViewAttendanceHistoryInputBoundary {
 private final ViewAttendanceHistoryDataAccessInterface dao;
 private final ViewAttendanceHistoryOutputBoundary presenter;
 private final CurrentUserInterface currentUser;
 public ViewAttendanceHistoryInteractor(ViewAttendanceHistoryDataAccessInterface dao,
         ViewAttendanceHistoryOutputBoundary presenter, CurrentUserInterface currentUser) {
  this.dao = dao; this.presenter = presenter; this.currentUser = currentUser;
 }
 @Override public void execute(ViewAttendanceHistoryInputData data) {
  try {
   final User user = dao.get(currentUser.getCurrentUsername());
   if (user == null || user.getRole() != Role.PLAYER) {
    presenter.prepareFailView("Sign in as a player to see attendance history."); return;
   }
   List<AttendanceHistoryRow> rows = new ArrayList<>();
   for (AttendanceRecord record : dao.getAttendanceForPlayer(user.getUsername())) {
    Event event = dao.getEvent(record.getEventId());
    if (event != null) {
     rows.add(new AttendanceHistoryRow(event.getEventId(), event.getDateTime(),
         event.getType().name(), record.getStatus(), record.getStatsNotes()));
    }
   }
   rows.sort(Comparator.comparing(AttendanceHistoryRow::getDate).reversed());
   presenter.prepareSuccessView(new ViewAttendanceHistoryOutputData(rows));
  }
  catch (IllegalStateException | UncheckedIOException ex) {
   presenter.prepareFailView("Cannot load history: " + ex.getMessage());
  }
 }
}
