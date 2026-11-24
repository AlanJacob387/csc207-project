package interface_adapter.browse_events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import use_case.browse_events.BrowseEventsOutputData;

/**
 * Tests the Browse Events presenter, mainly that a row with no date is shown
 * instead of crashing the list.
 */
class BrowseEventsPresenterTest {

    private BrowseEventsViewModel viewModel;
    private BrowseEventsPresenter presenter;

    @BeforeEach
    void setUp() {
        viewModel = new BrowseEventsViewModel();
        presenter = new BrowseEventsPresenter(viewModel);
    }

    @Test
    void eventWithNoDateIsShownWithoutDateOrTime() {
        final BrowseEventsOutputData.EventRow dated = new BrowseEventsOutputData.EventRow(
                "e1", "hawks", "Hawks", "GAME", LocalDateTime.of(2026, 12, 3, 18, 0),
                "North Court", "Raptors", false);
        final BrowseEventsOutputData.EventRow undated = new BrowseEventsOutputData.EventRow(
                "e2", "hawks", "Hawks", "PRACTICE", null, "Main Gym", null, false);

        presenter.prepareSuccessView(new BrowseEventsOutputData(List.of(dated, undated),
                List.of(new BrowseEventsOutputData.TeamOption("hawks", "Hawks")), true, false));

        assertFalse(viewModel.isError());
        final List<BrowseEventsViewModel.Entry> entries = viewModel.getEntries();
        assertEquals(2, entries.size());
        assertEquals("Game vs Raptors", entries.get(0).getTitle());
        assertFalse(entries.get(0).getTime().isEmpty());

        assertEquals("e2", entries.get(1).getEventId());
        assertEquals("Date not set", entries.get(1).getDate());
        assertEquals("", entries.get(1).getTime());
        assertEquals("Practice", entries.get(1).getTitle());
    }

    @Test
    void emptyFilteredListGetsNoMatchMessage() {
        presenter.prepareSuccessView(new BrowseEventsOutputData(List.of(),
                List.of(new BrowseEventsOutputData.TeamOption("hawks", "Hawks")), true, true));

        assertTrue(viewModel.getEntries().isEmpty());
        assertEquals("No events match these filters.", viewModel.getMessage());
    }
}
