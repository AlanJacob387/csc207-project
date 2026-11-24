package interface_adapter.view_event_details;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import use_case.view_event_details.ViewEventDetailsOutputData;

/**
 * Tests the View Event Details presenter, mainly that an event with no date
 * opens instead of crashing the dialog.
 */
class ViewEventDetailsPresenterTest {

    private ViewEventDetailsViewModel viewModel;
    private ViewEventDetailsPresenter presenter;

    @BeforeEach
    void setUp() {
        viewModel = new ViewEventDetailsViewModel();
        presenter = new ViewEventDetailsPresenter(viewModel);
    }

    @Test
    void eventWithNoDateShowsDateNotSetAndBlankTime() {
        presenter.prepareSuccessView(new ViewEventDetailsOutputData(
                "e1", "Hawks", "PRACTICE", null, "Main Gym", null, false, true));

        assertEquals("", viewModel.getErrorMessage());
        assertEquals("Practice", viewModel.getTitle());
        assertEquals("Date not set", viewModel.getDate());
        assertEquals("", viewModel.getTime());
        assertNull(viewModel.getDateTime());
        assertEquals("Main Gym", viewModel.getLocation());
    }

    @Test
    void datedGameShowsDateTimeAndOpponent() {
        final LocalDateTime when = LocalDateTime.of(2026, 12, 3, 18, 0);
        presenter.prepareSuccessView(new ViewEventDetailsOutputData(
                "e2", "Hawks", "GAME", when, "North Court", "Raptors", false, false));

        assertEquals("Game vs Raptors", viewModel.getTitle());
        assertFalse(viewModel.getDate().isEmpty());
        assertFalse(viewModel.getTime().isEmpty());
        assertEquals(when, viewModel.getDateTime());
    }
}
