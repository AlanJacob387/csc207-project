package use_case.edit_event;

/**
 * Output boundary for the Edit Event use case.
 */
public interface EditEventOutputBoundary {

    /**
     * Reports that the change was saved.
     *
     * @param outputData which event changed
     */
    void prepareSuccessView(EditEventOutputData outputData);

    /**
     * Reports why nothing was changed.
     *
     * @param errorMessage message for the user
     */
    void prepareFailView(String errorMessage);
}
