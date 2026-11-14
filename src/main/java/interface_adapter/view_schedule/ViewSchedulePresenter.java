package interface_adapter.view_schedule;

import java.util.ArrayList;
import java.util.List;

import use_case.view_schedule.ViewScheduleOutputBoundary;
import use_case.view_schedule.ViewScheduleOutputData;

/**
 * Presenter for the View Schedule use case.
 * Turns event rows into calendar entries with display titles.
 */
public class ViewSchedulePresenter implements ViewScheduleOutputBoundary {

    private static final String NO_TEAM_MESSAGE = "You're not on any team yet.";

    private final ViewScheduleViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the calendar's view model
     */
    public ViewSchedulePresenter(ViewScheduleViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ViewScheduleOutputData outputData) {
        if (!outputData.isOnAnyTeam()) {
            viewModel.setSchedule(new ArrayList<>(), NO_TEAM_MESSAGE);
            return;
        }

        final List<ViewScheduleViewModel.Entry> entries = new ArrayList<>();
        for (ViewScheduleOutputData.EventRow row : outputData.getEvents()) {
            entries.add(new ViewScheduleViewModel.Entry(
                    row.getEventId(),
                    titleFor(row),
                    "GAME".equals(row.getType()),
                    row.getDateTime().toLocalDate(),
                    row.getDateTime().toLocalTime()));
        }
        viewModel.setSchedule(entries, "");
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setError(errorMessage);
    }

    /** "Practice", "Game" or "Game vs Rivals", flagged when cancelled. */
    private static String titleFor(ViewScheduleOutputData.EventRow row) {
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
        // Cancelled events stay on the calendar so players notice the change.
        if (row.isCancelled()) {
            title = "Cancelled: " + title;
        }
        return title;
    }
}
