package use_case.browse_events;

/**
 * Output boundary for the Browse Events use case.
 */
public interface BrowseEventsOutputBoundary {

    /**
     * Shows the matching events.
     *
     * @param outputData the events, the user's teams, and why the list might be empty
     */
    void prepareSuccessView(BrowseEventsOutputData outputData);

    /**
     * Shows why the events could not be listed.
     *
     * @param errorMessage message for the user
     */
    void prepareFailView(String errorMessage);
}
