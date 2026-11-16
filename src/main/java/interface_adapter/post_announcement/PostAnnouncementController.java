package interface_adapter.post_announcement;

import interface_adapter.Session;
import use_case.post_announcement.PostAnnouncementInputBoundary;
import use_case.post_announcement.PostAnnouncementInputData;

/** Submits a post using the signed-in identity rather than an editable author field. */
public class PostAnnouncementController {
    private final PostAnnouncementInputBoundary interactor;
    private final Session session;

    /** Connects the posting use case to the shared session. */
    public PostAnnouncementController(PostAnnouncementInputBoundary interactor, Session session) {
        this.interactor = interactor;
        this.session = session;
    }

    /** Attempts to post the entered message to the selected team. */
    public void execute(String teamId, String message) {
        final String username = session.isLoggedIn() ? session.getCurrentUsername() : "";
        interactor.execute(new PostAnnouncementInputData(username, teamId, message));
    }
}
