package use_case.rsvp;
import java.util.List;
public final class RsvpOutputData {
 private final String message;
 private final List<String> responses;
 public RsvpOutputData(String message, List<String> responses) {
  this.message = message; this.responses = List.copyOf(responses);
 }
 public String getMessage() { return message; }
 public List<String> getResponses() { return responses; }
}
