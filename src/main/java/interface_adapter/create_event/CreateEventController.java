package interface_adapter.create_event;

import java.time.LocalDateTime;

import use_case.create_event.CreateEventInputBoundary;
import use_case.create_event.CreateEventInputData;

/**
 * Controller for the Create Event use case.
 * Called by the view; converts raw input into use-case input data.
 */
public class CreateEventController {
    private final CreateEventInputBoundary interactor;

    public CreateEventController(CreateEventInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void execute(String teamId, String eventType, LocalDateTime dateTime,
                        String location, String opponent) {
        interactor.execute(new CreateEventInputData(teamId, eventType, dateTime, location, opponent));
    }
}