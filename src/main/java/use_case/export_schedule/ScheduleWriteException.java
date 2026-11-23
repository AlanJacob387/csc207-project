package use_case.export_schedule;

/**
 * Thrown by a {@link ScheduleFileWriter} when the export file can't be written.
 * The message is meant to be shown to the user as is.
 */
public class ScheduleWriteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what went wrong, readable by the user
     * @param cause   the underlying error, or null
     */
    public ScheduleWriteException(String message, Throwable cause) {
        super(message, cause);
    }
}
