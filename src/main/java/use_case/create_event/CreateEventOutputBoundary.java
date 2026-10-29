package use_case.create_event;

/**
 * Output boundary for the Create Event use case.
 */
public interface CreateEventOutputBoundary {
    void prepareSuccessView(CreateEventOutputData outputData);

    void prepareFailView(String errorMessage);
}