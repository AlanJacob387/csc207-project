package interface_adapter.browse_events;

import use_case.browse_events.BrowseEventsInputBoundary;
import use_case.browse_events.BrowseEventsInputData;

/**
 * Controller for the Browse Events use case.
 * Called when the Events page is shown and whenever a filter changes.
 */
public class BrowseEventsController {
    private final BrowseEventsInputBoundary interactor;

    /**
     * Creates the controller.
     *
     * @param interactor the use case to run
     */
    public BrowseEventsController(BrowseEventsInputBoundary interactor) {
        this.interactor = interactor;
    }

    /**
     * Lists events matching the filters.
     *
     * @param type   "PRACTICE", "GAME", or null for every type
     * @param teamId a team id, or null for all of the user's teams
     */
    public void execute(String type, String teamId) {
        interactor.execute(new BrowseEventsInputData(type, teamId));
    }
}
