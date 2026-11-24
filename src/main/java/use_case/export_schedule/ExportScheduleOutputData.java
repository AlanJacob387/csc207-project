package use_case.export_schedule;

/**
 * Output data for the Export Schedule use case.
 */
public class ExportScheduleOutputData {
    private final String targetPath;
    private final int eventCount;
    private final int skippedCount;

    /**
     * Creates the output data.
     *
     * @param targetPath   the file that was written
     * @param eventCount   how many events it contains
     * @param skippedCount how many events were left out because they have no date
     */
    public ExportScheduleOutputData(String targetPath, int eventCount, int skippedCount) {
        this.targetPath = targetPath;
        this.eventCount = eventCount;
        this.skippedCount = skippedCount;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public int getEventCount() {
        return eventCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }
}
