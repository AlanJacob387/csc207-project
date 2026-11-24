package use_case.rsvp;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import entity.*;
import use_case.current_user.CurrentUserInterface;
/** Enforces event timing, team membership, and signed-in identity. */
public final class RsvpInteractor implements RsvpInputBoundary {
 private final RsvpDataAccessInterface dao;
 private final RsvpOutputBoundary presenter;
 private final CurrentUserInterface currentUser;
 private final Clock clock;
 public RsvpInteractor(RsvpDataAccessInterface dao, RsvpOutputBoundary presenter,
         CurrentUserInterface currentUser) {
  this(dao, presenter, currentUser, Clock.systemDefaultZone());
 }
 public RsvpInteractor(RsvpDataAccessInterface dao, RsvpOutputBoundary presenter,
         CurrentUserInterface currentUser, Clock clock) {
  this.dao = dao; this.presenter = presenter; this.currentUser = currentUser; this.clock = clock;
 }
 @Override public void execute(RsvpInputData input) {
  if (input == null || input.getEventId() == null || input.getEventId().isBlank()) {
   presenter.prepareFailView("Select an event."); return;
  }
  try {
   User user = dao.get(currentUser.getCurrentUsername());
   Event event = dao.getEvent(input.getEventId());
   Team team = event == null ? null : dao.getTeam(event.getTeamId());
   if (user == null || team == null || event.isCancelled()) {
    presenter.prepareFailView("Sign in and select an active event."); return;
   }
   if (input.getResponse() == null) {
    if (user.getRole() != Role.COACH || !user.getUsername().equals(team.getCoachUsername())) {
     presenter.prepareFailView("Only the team's coach can view all responses."); return;
    }
    List<String> rows = new ArrayList<>();
    for (RsvpRecord record : dao.getRsvpsForEvent(event.getEventId())) {
     rows.add(record.getPlayerUsername() + ": " + record.getResponse());
    }
    presenter.prepareSuccessView(new RsvpOutputData("Team RSVP responses", rows));
    return;
   }
   if (user.getRole() != Role.PLAYER || !team.getPlayerUsernames().contains(user.getUsername())) {
    presenter.prepareFailView("Only players on this team can RSVP."); return;
   }
   if (event.getDateTime() == null || !event.getDateTime().isAfter(LocalDateTime.now(clock))) {
    presenter.prepareFailView("RSVPs are only allowed for future events."); return;
   }
   if (input.getResponse() != RsvpRecord.Response.YES
           && input.getResponse() != RsvpRecord.Response.NO) {
    presenter.prepareFailView("Choose YES or NO for your RSVP."); return;
   }
   dao.saveRsvp(new RsvpRecord(event.getEventId(), user.getUsername(), input.getResponse()));
   presenter.prepareSuccessView(new RsvpOutputData("RSVP saved.", List.of()));
  }
  catch (IllegalStateException | UncheckedIOException ex) {
   presenter.prepareFailView("Could not save or load RSVP: " + ex.getMessage());
  }
 }
}
