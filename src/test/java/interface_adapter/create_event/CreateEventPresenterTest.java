package interface_adapter.create_event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import use_case.create_event.CreateEventOutputData;

/**
 * Tests the Create Event presenter and view model together: a result must
 * replace the previous one, since the view shows the error whenever there is
 * one.
 */
class CreateEventPresenterTest {

    private CreateEventViewModel viewModel;
    private CreateEventPresenter presenter;

    @BeforeEach
    void setUp() {
        viewModel = new CreateEventViewModel();
        presenter = new CreateEventPresenter(viewModel);
    }

    @Test
    void successAfterFailureClearsTheOldError() {
        presenter.prepareFailView("Location is required.");
        presenter.prepareSuccessView(new CreateEventOutputData("e1", "Event created."));

        assertEquals("", viewModel.getErrorMessage());
        assertEquals("Event created.", viewModel.getSuccessMessage());
    }

    @Test
    void failureAfterSuccessClearsTheOldSuccess() {
        presenter.prepareSuccessView(new CreateEventOutputData("e1", "Event created."));
        presenter.prepareFailView("Location is required.");

        assertEquals("Location is required.", viewModel.getErrorMessage());
        assertEquals("", viewModel.getSuccessMessage());
    }
}
