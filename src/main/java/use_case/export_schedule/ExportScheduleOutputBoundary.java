package use_case.export_schedule;

/**
 * Output boundary for the Export Schedule use case.
 */
public interface ExportScheduleOutputBoundary {

    /**
     * Reports that the file was written.
     *
     * @param outputData where it went and how many events it holds
     */
    void prepareSuccessView(ExportScheduleOutputData outputData);

    /**
     * Reports that there was nothing to export, so no file was written. This
     * is not an error, so it gets its own method rather than the fail view.
     *
     * @param message message for the user
     */
    void prepareNothingToExportView(String message);

    /**
     * Reports why the export failed.
     *
     * @param errorMessage message for the user
     */
    void prepareFailView(String errorMessage);
}
