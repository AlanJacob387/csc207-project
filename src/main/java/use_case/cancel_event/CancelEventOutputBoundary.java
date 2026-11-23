package use_case.cancel_event;

/**
 * Output boundary for the Cancel Event use case.
 */
public interface CancelEventOutputBoundary {

    /**
     * Reports that the event is now cancelled.
     *
     * @param outputData which event was cancelled
     */
    void prepareSuccessView(CancelEventOutputData outputData);

    /**
     * Reports why the event was not cancelled.
     *
     * @param errorMessage message for the user
     */
    void prepareFailView(String errorMessage);
}
