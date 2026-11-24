package use_case.view_schedule;

/**
 * Output boundary for the View Schedule use case.
 */
public interface ViewScheduleOutputBoundary {

    /**
     * Shows the events in the requested range.
     *
     * @param outputData the events, plus whether the user is on any team
     */
    void prepareSuccessView(ViewScheduleOutputData outputData);

    /**
     * Shows why the schedule could not be loaded.
     *
     * @param errorMessage message for the user
     */
    void prepareFailView(String errorMessage);
}
