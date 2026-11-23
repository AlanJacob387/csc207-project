package entity;
/** A player's intended attendance before an event; distinct from actual attendance. */
public final class RsvpRecord {
 public enum Response { YES, NO, MAYBE }
 private final String eventId;
 private final String playerUsername;
 private final Response response;
 public RsvpRecord(String eventId, String playerUsername, Response response) {
  this.eventId = eventId; this.playerUsername = playerUsername; this.response = response;
 }
 public String getEventId() { return eventId; }
 public String getPlayerUsername() { return playerUsername; }
 public Response getResponse() { return response; }
}
