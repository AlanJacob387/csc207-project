package interface_adapter.submit_absence_notice;

import use_case.submit_absence_notice.SubmitAbsenceNoticeInputBoundary;
import use_case.submit_absence_notice.SubmitAbsenceNoticeInputData;

/** Passes form input to the use case, which reads the current session itself. */
public class SubmitAbsenceNoticeController {
    private final SubmitAbsenceNoticeInputBoundary interactor;

    public SubmitAbsenceNoticeController(SubmitAbsenceNoticeInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void execute(String playerUsername, String eventId, String reason) {
        interactor.execute(new SubmitAbsenceNoticeInputData(playerUsername, eventId, reason));
    }
}
