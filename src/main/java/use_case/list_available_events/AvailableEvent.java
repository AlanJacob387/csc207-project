package use_case.list_available_events;
/** Read-only event option for the user interface. */
public final class AvailableEvent {
 private final String eventId;
 private final String label;
 public AvailableEvent(String eventId, String label) { this.eventId = eventId; this.label = label; }
 public String getEventId() { return eventId; }
 @Override public String toString() { return label; }
}
