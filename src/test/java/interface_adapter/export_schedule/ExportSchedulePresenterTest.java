package interface_adapter.export_schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import use_case.export_schedule.ExportScheduleOutputData;

/**
 * Tests the Export Schedule presenter's success message, including the note
 * about events left out for having no date.
 */
class ExportSchedulePresenterTest {

    private ExportScheduleViewModel viewModel;
    private ExportSchedulePresenter presenter;

    @BeforeEach
    void setUp() {
        viewModel = new ExportScheduleViewModel();
        presenter = new ExportSchedulePresenter(viewModel);
    }

    @Test
    void successMentionsEventsLeftOut() {
        presenter.prepareSuccessView(new ExportScheduleOutputData("team.ics", 3, 2));

        assertEquals(ExportScheduleViewModel.Status.SUCCESS, viewModel.getStatus());
        assertTrue(viewModel.getMessage().startsWith("Exported 3 events to team.ics"));
        assertTrue(viewModel.getMessage().contains("Left out 2 events with no date set."));
    }

    @Test
    void successWithNothingLeftOutHasNoNote() {
        presenter.prepareSuccessView(new ExportScheduleOutputData("team.ics", 1, 0));

        assertTrue(viewModel.getMessage().startsWith("Exported 1 event to team.ics"));
        assertFalse(viewModel.getMessage().contains("Left out"));
    }
}
