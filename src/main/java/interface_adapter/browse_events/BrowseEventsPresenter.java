package interface_adapter.browse_events;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import use_case.browse_events.BrowseEventsOutputBoundary;
import use_case.browse_events.BrowseEventsOutputData;

/**
 * Presenter for the Browse Events use case.
 * Formats each event as a list row and picks the right empty-list message.
 */
public class BrowseEventsPresenter implements BrowseEventsOutputBoundary {

    private static final String NO_TEAM_MESSAGE = "You're not on any team yet.";
    private static final String NO_MATCH_MESSAGE = "No events match these filters.";
    private static final String NO_EVENTS_MESSAGE = "No events scheduled yet.";

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault());
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());

    private final BrowseEventsViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the Events page's view model
     */
    public BrowseEventsPresenter(BrowseEventsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(BrowseEventsOutputData outputData) {
        final List<BrowseEventsViewModel.TeamChoice> teams = new ArrayList<>();
        for (BrowseEventsOutputData.TeamOption team : outputData.getTeams()) {
            teams.add(new BrowseEventsViewModel.TeamChoice(team.getTeamId(), team.getName()));
        }

        final List<BrowseEventsViewModel.Entry> entries = new ArrayList<>();
        for (BrowseEventsOutputData.EventRow row : outputData.getEvents()) {
            entries.add(new BrowseEventsViewModel.Entry(
                    row.getEventId(),
                    row.getDateTime().format(DATE_FORMAT),
                    row.getDateTime().format(TIME_FORMAT),
                    titleFor(row),
                    row.getTeamName(),
                    row.getLocation() == null ? "" : row.getLocation(),
                    row.isCancelled()));
        }

        final String message;
        if (!outputData.isOnAnyTeam()) {
            message = NO_TEAM_MESSAGE;
        }
        else if (entries.isEmpty()) {
            message = outputData.isFiltered() ? NO_MATCH_MESSAGE : NO_EVENTS_MESSAGE;
        }
        else {
            message = "";
        }
        viewModel.setEvents(entries, teams, message);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setError(errorMessage);
    }

    /** "Practice", "Game" or "Game vs Rivals", flagged when cancelled. */
    private static String titleFor(BrowseEventsOutputData.EventRow row) {
        String title;
        if ("GAME".equals(row.getType())) {
            title = "Game";
            if (row.getOpponent() != null && !row.getOpponent().isBlank()) {
                title += " vs " + row.getOpponent();
            }
        }
        else {
            title = "Practice";
        }
        // Same wording as the calendar, so cancelled events read the same everywhere.
        if (row.isCancelled()) {
            title = "Cancelled: " + title;
        }
        return title;
    }
}
