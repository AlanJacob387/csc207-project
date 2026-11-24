package use_case.create_event;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.UUID;

import entity.Event;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Create Event use case.
 *
 * Only the coach of a team may schedule events for it. The signed-in user
 * comes from the current-user port, never from the view, so the check holds no
 * matter which page calls this.
 */
public class CreateEventInteractor implements CreateEventInputBoundary {
    private final CreateEventDataAccessInterface dataAccess;
    private final CreateEventOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where teams are checked and events are saved
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public CreateEventInteractor(CreateEventDataAccessInterface dataAccess,
                                 CreateEventOutputBoundary presenter,
                                 CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(CreateEventInputData inputData) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in to create events.");
            return;
        }

        final boolean canManage;
        try {
            if (!dataAccess.teamExists(inputData.getTeamId())) {
                presenter.prepareFailView("Team does not exist: " + inputData.getTeamId());
                return;
            }
            canManage = dataAccess.canManageEvent(username, inputData.getTeamId());
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not read the team: " + ex.getMessage());
            return;
        }
        if (!canManage) {
            presenter.prepareFailView("Only the team's coach can create events for this team.");
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

        // valueOf(null) throws NullPointerException rather than
        // IllegalArgumentException, so a missing type is checked first.
        if (inputData.getEventType() == null || inputData.getEventType().isBlank()) {
            presenter.prepareFailView("Choose an event type.");
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
                opponentFor(type, inputData.getOpponent())
        );

        try {
            dataAccess.saveEvent(event);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the event: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(
                new CreateEventOutputData(event.getEventId(), "Event created."));
    }

    /**
     * Practices have no opponent; a blank one on a game means "not decided yet".
     * Same rule as editing an event, so both store the same thing.
     */
    private static String opponentFor(Event.EventType type, String opponent) {
        if (type != Event.EventType.GAME || opponent == null || opponent.isBlank()) {
            return null;
        }
        return opponent.trim();
    }
}
