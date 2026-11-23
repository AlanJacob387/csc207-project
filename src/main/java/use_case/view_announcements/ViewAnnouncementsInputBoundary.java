package use_case.view_announcements;

/** Accepts a request to browse a signed-in user's team announcements. */
public interface ViewAnnouncementsInputBoundary {
    void execute(ViewAnnouncementsInputData inputData);
}
