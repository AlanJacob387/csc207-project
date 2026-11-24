package use_case.export_schedule;

/**
 * Input data for the Export Schedule use case.
 *
 * The target is a plain string so this layer never deals with file-system
 * types; only the file writer gateway turns it into a real path.
 */
public class ExportScheduleInputData {
    private final String targetPath;

    /**
     * Creates the input data.
     *
     * @param targetPath the file the user chose to export to
     */
    public ExportScheduleInputData(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetPath() {
        return targetPath;
    }
}
