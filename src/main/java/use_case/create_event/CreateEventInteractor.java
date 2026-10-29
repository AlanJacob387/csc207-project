package use_case.create_event;

import java.time.LocalDateTime;
import java.util.UUID;

import entity.Event;

/**
 * Interactor for the Create Event use case.
 */
public class CreateEventInteractor implements CreateEventInputBoundary {
    private final CreateEventDataAccessInterface dataAccess;
    private final CreateEventOutputBoundary presenter;

    public CreateEventInteractor(CreateEventDataAccessInterface dataAccess,
                                 CreateEventOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(CreateEventInputData inputData) {
        if (!dataAccess.teamExists(inputData.getTeamId())) {
            presenter.prepareFailView("Team does not exist: " + inputData.getTeamId());
            return;
        }
        if (inputData.getDateTime() == null || inputData.getDateTime().isBefore(LocalDateTime.now())) {
            presenter.prepareFailView("Event date/time must be in the future.");
            return;
        }
        if (inputData.getLocation() == null || inputData.getLocation().isBlank()) {
            presenter.prepareFailView("Location is required.");
            return;
        }

        final Event.EventType type;
        try {
            type = Event.EventType.valueOf(inputData.getEventType());
        } catch (IllegalArgumentException e) {
            presenter.prepareFailView("Unknown event type: " + inputData.getEventType());
            return;
        }

        final Event event = new Event(
                UUID.randomUUID().toString(),
                inputData.getTeamId(),
                type,
                inputData.getDateTime(),
                inputData.getLocation(),
                inputData.getOpponent()
        );

        dataAccess.saveEvent(event);
        presenter.prepareSuccessView(
                new CreateEventOutputData(event.getEventId(), "Event created."));
    }
}