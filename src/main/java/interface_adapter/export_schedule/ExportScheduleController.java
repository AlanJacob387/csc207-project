package interface_adapter.export_schedule;

import use_case.export_schedule.ExportScheduleInputBoundary;
import use_case.export_schedule.ExportScheduleInputData;

/**
 * Controller for the Export Schedule use case.
 * Called by the calendar once the user has picked where to save the file.
 */
public class ExportScheduleController {
    private final ExportScheduleInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case to run
     */
    public ExportScheduleController(ExportScheduleInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Exports the schedule.
     *
     * @param targetPath the file the user chose
     */
    public void execute(String targetPath) {
        interactor.execute(new ExportScheduleInputData(targetPath));
    }
}
