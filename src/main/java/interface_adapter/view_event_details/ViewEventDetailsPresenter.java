package interface_adapter.view_event_details;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import use_case.view_event_details.ViewEventDetailsOutputBoundary;
import use_case.view_event_details.ViewEventDetailsOutputData;

/**
 * Presenter for the View Event Details use case.
 * Formats the event for the details dialog.
 */
public class ViewEventDetailsPresenter implements ViewEventDetailsOutputBoundary {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault());
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
    private static final String NO_DATE_TEXT = "Date not set";

    private final ViewEventDetailsViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the details dialog's view model
     */
    public ViewEventDetailsPresenter(ViewEventDetailsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ViewEventDetailsOutputData outputData) {
        final boolean game = "GAME".equals(outputData.getType());
        final String opponent = outputData.getOpponent() == null ? "" : outputData.getOpponent();

        String title = game ? "Game" : "Practice";
        if (game && !opponent.isBlank()) {
            title += " vs " + opponent;
        }

        // Same wording as the Events list, which is where a dateless event can
        // be opened from. The raw value stays null so the edit form starts blank.
        final LocalDateTime when = outputData.getDateTime();
        viewModel.setDetails(
                outputData.getEventId(),
                title,
                outputData.getTeamName(),
                when == null ? NO_DATE_TEXT : when.format(DATE_FORMAT),
                when == null ? "" : when.format(TIME_FORMAT),
                when,
                outputData.getLocation() == null ? "" : outputData.getLocation(),
                opponent,
                game,
                outputData.isCancelled(),
                outputData.isCanManage());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setErrorMessage(errorMessage);
    }
}
