package interface_adapter.view_absence_notices;

import use_case.view_absence_notices.ViewAbsenceNoticesInputBoundary;
import use_case.view_absence_notices.ViewAbsenceNoticesInputData;

/** Passes form input to the use case, which reads the current session itself. */
public class ViewAbsenceNoticesController {
    private final ViewAbsenceNoticesInputBoundary interactor;

    public ViewAbsenceNoticesController(ViewAbsenceNoticesInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void execute(String playerUsername) {
        interactor.execute(new ViewAbsenceNoticesInputData(playerUsername));
    }
}
