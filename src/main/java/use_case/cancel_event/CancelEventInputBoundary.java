package use_case.cancel_event;

/**
 * Input boundary for the Cancel Event use case.
 */
public interface CancelEventInputBoundary {

    /**
     * Marks an event as cancelled.
     *
     * @param inputData which event to cancel
     */
    void execute(CancelEventInputData inputData);
}
