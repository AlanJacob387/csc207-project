package use_case.browse_events;

/**
 * Input boundary for the Browse Events use case.
 */
public interface BrowseEventsInputBoundary {

    /**
     * Lists the signed-in user's events that match the filters.
     *
     * @param inputData the filters to apply
     */
    void execute(BrowseEventsInputData inputData);
}
