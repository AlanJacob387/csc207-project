package use_case.view_stats;
import java.io.UncheckedIOException;
import java.util.List;
import entity.*;
import use_case.current_user.CurrentUserInterface;
public final class ViewStatsInteractor implements ViewStatsInputBoundary {
 private final ViewStatsDataAccessInterface dao;
 private final ViewStatsOutputBoundary presenter;
 private final CurrentUserInterface session;
 public ViewStatsInteractor(ViewStatsDataAccessInterface dao, ViewStatsOutputBoundary presenter,
         CurrentUserInterface session) {
  this.dao = dao; this.presenter = presenter; this.session = session;
 }
 @Override public void execute(ViewStatsInputData input) {
  try {
   final User user = dao.get(session.getCurrentUsername());
   if (user == null || user.getRole() != Role.PLAYER) {
    presenter.prepareFailView("Sign in as a player to see your stats."); return;
   }
   int present = 0, absent = 0, excused = 0;
   for (AttendanceRecord record : dao.getAttendanceForPlayer(user.getUsername())) {
    switch (record.getStatus()) {
     case PRESENT -> present++;
     case ABSENT -> absent++;
     case EXCUSED -> excused++;
     default -> throw new IllegalStateException("Unexpected attendance status");
    }
   }
   presenter.prepareSuccessView(new ViewStatsOutputData(present, absent, excused));
  }
  catch (IllegalStateException | UncheckedIOException ex) {
   presenter.prepareFailView("Could not load stats: " + ex.getMessage());
  }
 }
}
