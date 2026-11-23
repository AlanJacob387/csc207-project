package use_case.list_available_events;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import entity.*;
import use_case.current_user.CurrentUserInterface;
/** Lists only events accessible to the signed-in coach or player. */
public final class ListAvailableEventsInteractor implements ListAvailableEventsInputBoundary {
 private final ListAvailableEventsDataAccessInterface dao;
 private final ListAvailableEventsOutputBoundary output;
 private final CurrentUserInterface currentUser;
 public ListAvailableEventsInteractor(ListAvailableEventsDataAccessInterface dao,
         ListAvailableEventsOutputBoundary output, CurrentUserInterface currentUser) {
  this.dao = dao; this.output = output; this.currentUser = currentUser;
 }
 @Override public void execute() {
  try {
   final User user = dao.get(currentUser.getCurrentUsername());
   if (user == null) { output.prepareFailView("Sign in to see your events."); return; }
   final List<AvailableEvent> options = new ArrayList<>();
   for (Event event : dao.getAllEvents()) {
    final Team team = dao.getTeam(event.getTeamId());
    if (team == null) { continue; }
    final boolean coach = user.getRole() == Role.COACH
            && user.getUsername().equals(team.getCoachUsername());
    final boolean player = user.getRole() == Role.PLAYER
            && team.getPlayerUsernames().contains(user.getUsername());
    if (coach || player) {
     options.add(new AvailableEvent(event.getEventId(),
             event.getDateTime() + " | " + event.getType() + " | " + team.getName()
             + (event.isCancelled() ? " [cancelled]" : "")));
    }
   }
   output.prepareSuccessView(options);
  }
  catch (IllegalStateException | UncheckedIOException ex) {
   output.prepareFailView("Could not list events: " + ex.getMessage());
  }
 }
}
