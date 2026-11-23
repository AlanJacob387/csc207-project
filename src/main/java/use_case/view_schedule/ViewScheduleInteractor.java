package use_case.view_schedule;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import entity.Event;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the View Schedule use case.
 *
 * Loads the events of every team the signed-in user belongs to for the range
 * the calendar is showing. The user comes from the current-user port rather
 * than from the view, so nobody can ask for another user's schedule.
 */
public class ViewScheduleInteractor implements ViewScheduleInputBoundary {

    private final ViewScheduleDataAccessInterface dataAccess;
    private final ViewScheduleOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where teams and events are read from
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public ViewScheduleInteractor(ViewScheduleDataAccessInterface dataAccess,
                                  ViewScheduleOutputBoundary presenter,
                                  CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(ViewScheduleInputData inputData) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in to see the schedule.");
            return;
        }
        if (inputData.getFrom() == null || inputData.getTo() == null) {
            presenter.prepareFailView("No date range was given.");
            return;
        }
        if (inputData.getFrom().isAfter(inputData.getTo())) {
            presenter.prepareFailView("The start of the range is after the end.");
            return;
        }

        final List<String> teamIds;
        final List<Event> events;
        try {
            teamIds = dataAccess.getTeamIdsForMember(username);
            if (teamIds.isEmpty()) {
                presenter.prepareSuccessView(new ViewScheduleOutputData(new ArrayList<>(), false));
                return;
            }
            events = dataAccess.getEventsForTeams(teamIds, inputData.getFrom(), inputData.getTo());
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not read the schedule: " + ex.getMessage());
            return;
        }

        // The store returns events in insertion order, but the calendar reads
        // best earliest first.
        // An event with no date has no cell on the calendar, so it is left out
        // here. The JSON store already drops them for a date range; this keeps
        // any other implementation from crashing the sort or the presenter.
        final List<Event> sorted = new ArrayList<>();
        for (Event event : events) {
            if (event.getDateTime() != null) {
                sorted.add(event);
            }
        }
        sorted.sort(Comparator.comparing(Event::getDateTime));

        final List<ViewScheduleOutputData.EventRow> rows = new ArrayList<>();
        for (Event event : sorted) {
            rows.add(new ViewScheduleOutputData.EventRow(
                    event.getEventId(),
                    event.getTeamId(),
                    event.getType().name(),
                    event.getDateTime(),
                    event.getLocation(),
                    event.getOpponent(),
                    event.isCancelled()));
        }
        presenter.prepareSuccessView(new ViewScheduleOutputData(rows, true));
    }
}
