package use_case.view_announcements;

/** Receives an authorized announcement list or a browsing error. */
public interface ViewAnnouncementsOutputBoundary {
    void prepareSuccessView(ViewAnnouncementsOutputData outputData);

    void prepareFailView(String errorMessage);
}
