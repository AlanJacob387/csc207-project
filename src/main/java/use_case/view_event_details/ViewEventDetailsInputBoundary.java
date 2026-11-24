package use_case.view_event_details;

/**
 * Input boundary for the View Event Details use case.
 */
public interface ViewEventDetailsInputBoundary {

    /**
     * Loads everything known about one event.
     *
     * @param inputData which event to show
     */
    void execute(ViewEventDetailsInputData inputData);
}
