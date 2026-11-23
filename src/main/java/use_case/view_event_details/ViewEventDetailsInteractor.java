package use_case.view_event_details;

import java.io.UncheckedIOException;

import entity.Event;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the View Event Details use case.
 *
 * Loads one event and works out whether the signed-in user coaches its team,
 * so the view knows whether to offer edit and cancel.
 */
public class ViewEventDetailsInteractor implements ViewEventDetailsInputBoundary {

    private final ViewEventDetailsDataAccessInterface dataAccess;
    private final ViewEventDetailsOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where events and teams are read from
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public ViewEventDetailsInteractor(ViewEventDetailsDataAccessInterface dataAccess,
                                      ViewEventDetailsOutputBoundary presenter,
                                      CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(ViewEventDetailsInputData inputData) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in to see event details.");
            return;
        }
        final String eventId = inputData.getEventId();
        if (eventId == null || eventId.isBlank()) {
            presenter.prepareFailView("No event was selected.");
            return;
        }

        final Event event;
        final String teamName;
        final String coach;
        try {
            event = dataAccess.getEvent(eventId);
            if (event == null) {
                presenter.prepareFailView("That event no longer exists.");
                return;
            }
            teamName = dataAccess.getTeamName(event.getTeamId());
            coach = dataAccess.getCoachUsername(event.getTeamId());
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not read the event: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(new ViewEventDetailsOutputData(
                event.getEventId(),
                // An event can outlive its team; the id is better than nothing.
                teamName != null ? teamName : event.getTeamId(),
                event.getType().name(),
                event.getDateTime(),
                event.getLocation(),
                event.getOpponent(),
                event.isCancelled(),
                username.equals(coach)));
    }
}
