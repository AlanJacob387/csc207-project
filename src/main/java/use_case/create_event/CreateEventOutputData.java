package use_case.create_event;

/**
 * Output data for the Create Event use case.
 */
public class CreateEventOutputData {
    private final String eventId;
    private final String message;

    public CreateEventOutputData(String eventId, String message) {
        this.eventId = eventId;
        this.message = message;
    }

    public String getEventId() {
        return eventId;
    }

    public String getMessage() {
        return message;
    }
}