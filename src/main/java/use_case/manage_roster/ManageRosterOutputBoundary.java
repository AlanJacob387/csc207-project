package use_case.manage_roster;

/** Receives the result after a roster change is attempted. */
public interface ManageRosterOutputBoundary {
    void prepareSuccessView(ManageRosterOutputData outputData);

    void prepareFailView(String errorMessage);
}
