package use_case.view_event_details;

/**
 * Output boundary for the View Event Details use case.
 */
public interface ViewEventDetailsOutputBoundary {

    /**
     * Shows the event.
     *
     * @param outputData the event's details
     */
    void prepareSuccessView(ViewEventDetailsOutputData outputData);

    /**
     * Shows why the event could not be loaded.
     *
     * @param errorMessage message for the user
     */
    void prepareFailView(String errorMessage);
}
