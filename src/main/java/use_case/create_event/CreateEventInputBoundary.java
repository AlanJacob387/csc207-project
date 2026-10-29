package use_case.create_event;

/**
 * Input boundary for the Create Event use case.
 */
public interface CreateEventInputBoundary {
    void execute(CreateEventInputData inputData);
}