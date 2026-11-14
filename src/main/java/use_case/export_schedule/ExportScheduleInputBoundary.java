package use_case.export_schedule;

/**
 * Input boundary for the Export Schedule use case.
 */
public interface ExportScheduleInputBoundary {

    /**
     * Writes the signed-in user's whole schedule to an iCalendar file.
     *
     * @param inputData where to write it
     */
    void execute(ExportScheduleInputData inputData);
}
