package use_case.view_absence_notices;

/** Receives an authorized result or an explanation of why the request failed. */
public interface ViewAbsenceNoticesOutputBoundary {
    void prepareSuccessView(ViewAbsenceNoticesOutputData outputData);
    void prepareFailView(String errorMessage);
}
