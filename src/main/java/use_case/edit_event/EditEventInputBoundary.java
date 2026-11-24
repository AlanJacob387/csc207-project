package use_case.edit_event;

/**
 * Input boundary for the Edit Event use case.
 */
public interface EditEventInputBoundary {

    /**
     * Changes an event's date/time, location and opponent.
     *
     * @param inputData the event and its new values
     */
    void execute(EditEventInputData inputData);
}
