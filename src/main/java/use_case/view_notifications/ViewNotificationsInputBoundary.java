package use_case.view_notifications;

/** Loads event changes for the signed-in user's permitted teams. */
public interface ViewNotificationsInputBoundary {
    void execute(ViewNotificationsInputData inputData);
}
