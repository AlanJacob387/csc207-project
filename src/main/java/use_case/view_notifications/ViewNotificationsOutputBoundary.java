package use_case.view_notifications;

/** Receives an authorized feed or a reason the feed could not be loaded. */
public interface ViewNotificationsOutputBoundary {
    void prepareSuccessView(ViewNotificationsOutputData outputData);

    void prepareFailView(String errorMessage);
}
