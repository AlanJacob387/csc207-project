package use_case.edit_event;

/**
 * Output data for the Edit Event use case.
 */
public class EditEventOutputData {
    private final String eventId;
    private final String message;

    /**
     * Creates the output data.
     *
     * @param eventId the event that was changed
     * @param message confirmation for the user
     */
    public EditEventOutputData(String eventId, String message) {
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
