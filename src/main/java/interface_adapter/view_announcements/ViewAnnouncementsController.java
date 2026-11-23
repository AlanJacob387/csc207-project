package interface_adapter.view_announcements;

import interface_adapter.Session;
import use_case.view_announcements.ViewAnnouncementsInputBoundary;
import use_case.view_announcements.ViewAnnouncementsInputData;

/** Loads announcements using the current signed-in identity. */
public class ViewAnnouncementsController {
    private final ViewAnnouncementsInputBoundary interactor;
    private final Session session;

    /** Connects the reading use case to the shared session. */
    public ViewAnnouncementsController(ViewAnnouncementsInputBoundary interactor, Session session) {
        this.interactor = interactor;
        this.session = session;
    }

    /** Loads a selected team, or the first accessible team when none is selected. */
    public void execute(String teamId) {
        final String username = session.isLoggedIn() ? session.getCurrentUsername() : "";
        interactor.execute(new ViewAnnouncementsInputData(username, teamId));
    }
}
