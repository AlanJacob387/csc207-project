package interface_adapter.view_notifications;

import use_case.view_notifications.ViewNotificationsInputBoundary;
import use_case.view_notifications.ViewNotificationsInputData;

/** Passes a team filter to a use case that obtains identity from the session. */
public class ViewNotificationsController {
    private final ViewNotificationsInputBoundary interactor;

    public ViewNotificationsController(ViewNotificationsInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void execute(String teamId) {
        interactor.execute(new ViewNotificationsInputData(teamId));
    }
}
