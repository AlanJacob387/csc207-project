package use_case.view_schedule;

/**
 * Input boundary for the View Schedule use case.
 */
public interface ViewScheduleInputBoundary {

    /**
     * Loads the signed-in user's events for a date range.
     *
     * @param inputData the range being viewed
     */
    void execute(ViewScheduleInputData inputData);
}
