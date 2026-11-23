package interface_adapter.view_roster;

import interface_adapter.Session;
import use_case.view_roster.ViewRosterInputBoundary;
import use_case.view_roster.ViewRosterInputData;

/** Loads the rosters the signed-in user may see. */
public class ViewRosterController {
    private final ViewRosterInputBoundary interactor;
    private final Session session;

    public ViewRosterController(ViewRosterInputBoundary interactor, Session session) {
        this.interactor = interactor;
        this.session = session;
    }

    /** Reloads the roster list. */
    public void execute() {
        final String username = session.isLoggedIn() ? session.getCurrentUsername() : "";
        interactor.execute(new ViewRosterInputData(username));
    }
}
