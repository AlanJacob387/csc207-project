package interface_adapter.cancel_event;

import use_case.cancel_event.CancelEventInputBoundary;
import use_case.cancel_event.CancelEventInputData;

/**
 * Controller for the Cancel Event use case.
 * Called by the details dialog once the coach has confirmed.
 */
public class CancelEventController {
    private final CancelEventInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case to run
     */
    public CancelEventController(CancelEventInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Cancels an event.
     *
     * @param eventId the event to cancel
     */
    public void execute(String eventId) {
        interactor.execute(new CancelEventInputData(eventId));
    }
}
