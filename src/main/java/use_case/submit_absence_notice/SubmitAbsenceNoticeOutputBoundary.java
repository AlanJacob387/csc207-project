package use_case.submit_absence_notice;

/** Receives an authorized result or an explanation of why the request failed. */
public interface SubmitAbsenceNoticeOutputBoundary {
    void prepareSuccessView(SubmitAbsenceNoticeOutputData outputData);
    void prepareFailView(String errorMessage);
}
