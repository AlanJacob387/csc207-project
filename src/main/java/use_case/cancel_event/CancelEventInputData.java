package use_case.cancel_event;

/**
 * Input data for the Cancel Event use case.
 */
public class CancelEventInputData {
    private final String eventId;

    /**
     * Creates the input data.
     *
     * @param eventId the event to cancel
     */
    public CancelEventInputData(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
