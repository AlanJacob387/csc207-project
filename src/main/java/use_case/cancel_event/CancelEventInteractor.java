package use_case.cancel_event;

import java.io.UncheckedIOException;

import entity.Event;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Cancel Event use case.
 *
 * Only the coach of the event's team may cancel it. The event stays in the
 * store, flagged as cancelled, so players still see that it was called off.
 * Like editing, the flag is set on a copy so a failed save changes nothing.
 */
public class CancelEventInteractor implements CancelEventInputBoundary {

    private final CancelEventDataAccessInterface dataAccess;
    private final CancelEventOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where events are read from and saved to
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public CancelEventInteractor(CancelEventDataAccessInterface dataAccess,
                                 CancelEventOutputBoundary presenter,
                                 CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(CancelEventInputData inputData) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in to cancel events.");
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
            presenter.prepareFailView("Only the team's coach can cancel this event.");
            return;
        }
        if (original.isCancelled()) {
            presenter.prepareFailView("This event is already cancelled.");
            return;
        }

        final Event cancelled = copyOf(original);
        cancelled.cancel();

        try {
            dataAccess.updateEvent(cancelled);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the cancellation: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(
                new CancelEventOutputData(cancelled.getEventId(), "Event cancelled."));
    }

    /** A detached copy, so cancelling never touches the instance the store holds. */
    private static Event copyOf(Event event) {
        final Event copy = new Event(event.getEventId(), event.getTeamId(), event.getType(),
                event.getDateTime(), event.getLocation(), event.getOpponent());
        if (event.isCancelled()) {
            copy.cancel();
        }
        return copy;
    }
}
