package use_case.view_absence_notices;

/** Accepts a request for this parent-access use case. */
public interface ViewAbsenceNoticesInputBoundary {
    void execute(ViewAbsenceNoticesInputData inputData);
}
