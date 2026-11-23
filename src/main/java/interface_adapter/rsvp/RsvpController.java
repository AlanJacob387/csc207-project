package interface_adapter.rsvp;
import entity.RsvpRecord;
import use_case.rsvp.*;
public final class RsvpController {
 private final RsvpInputBoundary interactor;
 public RsvpController(RsvpInputBoundary interactor) { this.interactor = interactor; }
 public void execute(String eventId, RsvpRecord.Response response) {
  interactor.execute(new RsvpInputData(eventId, response));
 }
}
