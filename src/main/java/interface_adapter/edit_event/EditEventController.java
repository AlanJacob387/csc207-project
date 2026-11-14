package interface_adapter.edit_event;

import java.time.LocalDateTime;

import use_case.edit_event.EditEventInputBoundary;
import use_case.edit_event.EditEventInputData;

/**
 * Controller for the Edit Event use case.
 * Called by the details dialog when the coach saves their changes.
 */
public class EditEventController {
    private final EditEventInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case to run
     */
    public EditEventController(EditEventInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Saves new values for an event.
     *
     * @param eventId  the event being edited
     * @param dateTime new start date and time
     * @param location new location
     * @param opponent new opponent, or blank
     */
    public void execute(String eventId, LocalDateTime dateTime, String location, String opponent) {
        interactor.execute(new EditEventInputData(eventId, dateTime, location, opponent));
    }
}
