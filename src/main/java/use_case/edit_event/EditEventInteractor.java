package use_case.edit_event;

import java.io.UncheckedIOException;
import java.time.LocalDateTime;

import entity.Event;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Edit Event use case.
 *
 * Only the coach of the event's team may edit it. The change is made on a copy
 * of the event, never on the object the store handed back, so if saving fails
 * the event the app is showing is still the one on disk.
 */
public class EditEventInteractor implements EditEventInputBoundary {

    private final EditEventDataAccessInterface dataAccess;
    private final EditEventOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where events are read from and saved to
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public EditEventInteractor(EditEventDataAccessInterface dataAccess,
                               EditEventOutputBoundary presenter,
                               CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(EditEventInputData inputData) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in to edit events.");
            return;
        }

        final Event original;
        final String coach;
        try {
            original = dataAccess.getEvent(inputData.getEventId());
            if (original == null) {
                presenter.prepareFailView("That event no longer exists.");
                return;
            }
            coach = dataAccess.getCoachUsername(original.getTeamId());
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not read the event: " + ex.getMessage());
            return;
        }

        if (!username.equals(coach)) {
            presenter.prepareFailView("Only the team's coach can edit this event.");
            return;
        }
        if (original.isCancelled()) {
            presenter.prepareFailView("Cancelled events can't be edited.");
            return;
        }
        if (inputData.getLocation() == null || inputData.getLocation().isBlank()) {
            presenter.prepareFailView("Location is required.");
            return;
        }
        final LocalDateTime newDateTime = inputData.getDateTime();
        if (newDateTime == null) {
            presenter.prepareFailView("Enter a date and time.");
            return;
        }
        // Only a new date/time has to be in the future, so a coach can still
        // correct the location of an event that already happened.
        if (!newDateTime.equals(original.getDateTime())
                && newDateTime.isBefore(LocalDateTime.now())) {
            presenter.prepareFailView("Event date/time must be in the future.");
            return;
        }

        final Event updated = copyOf(original);
        updated.setDateTime(newDateTime);
        updated.setLocation(inputData.getLocation().trim());
        updated.setOpponent(opponentFor(original.getType(), inputData.getOpponent()));

        try {
            dataAccess.updateEvent(updated);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the change: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(new EditEventOutputData(updated.getEventId(), "Event updated."));
    }

    /** Practices have no opponent; a blank one on a game means "not decided yet". */
    private static String opponentFor(Event.EventType type, String opponent) {
        if (type != Event.EventType.GAME || opponent == null || opponent.isBlank()) {
            return null;
        }
        return opponent.trim();
    }

    /** A detached copy, so edits never touch the instance the store holds. */
    private static Event copyOf(Event event) {
        final Event copy = new Event(event.getEventId(), event.getTeamId(), event.getType(),
                event.getDateTime(), event.getLocation(), event.getOpponent());
        if (event.isCancelled()) {
            copy.cancel();
        }
        return copy;
    }
}
