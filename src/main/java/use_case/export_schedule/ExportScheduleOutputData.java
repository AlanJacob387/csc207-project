package use_case.export_schedule;

/**
 * Output data for the Export Schedule use case.
 */
public class ExportScheduleOutputData {
    private final String targetPath;
    private final int eventCount;

    /**
     * Creates the output data.
     *
     * @param targetPath the file that was written
     * @param eventCount how many events it contains
     */
    public ExportScheduleOutputData(String targetPath, int eventCount) {
        this.targetPath = targetPath;
        this.eventCount = eventCount;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public int getEventCount() {
        return eventCount;
    }
}
