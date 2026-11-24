package interface_adapter.view_event_details;

import use_case.view_event_details.ViewEventDetailsInputBoundary;
import use_case.view_event_details.ViewEventDetailsInputData;

/**
 * Controller for the View Event Details use case.
 * Called when an event on the calendar is clicked, and again after the event
 * changes so the details stay current.
 */
public class ViewEventDetailsController {
    private final ViewEventDetailsInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case to run
     */
    public ViewEventDetailsController(ViewEventDetailsInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Asks for one event's details.
     *
     * @param eventId the event to show
     */
    public void execute(String eventId) {
        interactor.execute(new ViewEventDetailsInputData(eventId));
    }
}
