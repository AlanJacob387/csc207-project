package use_case.export_schedule;

/**
 * Gateway for writing the exported calendar to disk.
 * Implemented in the data_access layer, so the interactor never touches the
 * file system itself.
 */
public interface ScheduleFileWriter {

    /**
     * Writes text to a file, replacing it if it already exists.
     *
     * @param targetPath the file to write
     * @param content    the full file content
     * @throws ScheduleWriteException if the file could not be written; the
     *                                message says why in plain words
     */
    void write(String targetPath, String content);
}
