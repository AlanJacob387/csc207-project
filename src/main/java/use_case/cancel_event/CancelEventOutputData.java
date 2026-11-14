package use_case.cancel_event;

/**
 * Output data for the Cancel Event use case.
 */
public class CancelEventOutputData {
    private final String eventId;
    private final String message;

    /**
     * Creates the output data.
     *
     * @param eventId the event that was cancelled
     * @param message confirmation for the user
     */
    public CancelEventOutputData(String eventId, String message) {
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
