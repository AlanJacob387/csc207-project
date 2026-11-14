package use_case.view_event_details;

/**
 * Input data for the View Event Details use case.
 */
public class ViewEventDetailsInputData {
    private final String eventId;

    /**
     * Creates the input data.
     *
     * @param eventId the event the user clicked on
     */
    public ViewEventDetailsInputData(String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }
}
