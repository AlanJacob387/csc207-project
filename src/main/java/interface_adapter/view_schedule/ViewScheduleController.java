package interface_adapter.view_schedule;

import java.time.LocalDate;

import use_case.view_schedule.ViewScheduleInputBoundary;
import use_case.view_schedule.ViewScheduleInputData;

/**
 * Controller for the View Schedule use case.
 * The calendar calls it whenever the month or week on screen changes.
 */
public class ViewScheduleController {
    private final ViewScheduleInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case to run
     */
    public ViewScheduleController(ViewScheduleInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Asks for the events between two dates, both inclusive.
     *
     * @param from first day shown
     * @param to   last day shown
     */
    public void execute(LocalDate from, LocalDate to) {
        interactor.execute(new ViewScheduleInputData(from, to));
    }
}
