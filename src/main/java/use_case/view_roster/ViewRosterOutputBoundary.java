package use_case.view_roster;

/** Receives the visible rosters, or the reason they cannot be shown. */
public interface ViewRosterOutputBoundary {
    void prepareSuccessView(ViewRosterOutputData outputData);

    void prepareFailView(String errorMessage);
}
