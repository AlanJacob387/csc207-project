package use_case.rsvp;
import entity.RsvpRecord;
/** Null response means coach is requesting the event's RSVP list. */
public final class RsvpInputData {
 private final String eventId;
 private final RsvpRecord.Response response;
 public RsvpInputData(String eventId, RsvpRecord.Response response) {
  this.eventId = eventId; this.response = response;
 }
 public String getEventId() { return eventId; }
 public RsvpRecord.Response getResponse() { return response; }
}
