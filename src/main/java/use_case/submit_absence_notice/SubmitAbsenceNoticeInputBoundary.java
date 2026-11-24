package use_case.submit_absence_notice;

/** Accepts a request for this parent-access use case. */
public interface SubmitAbsenceNoticeInputBoundary {
    void execute(SubmitAbsenceNoticeInputData inputData);
}
