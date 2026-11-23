package use_case.create_team;

/** Receives the result after creating a team is attempted. */
public interface CreateTeamOutputBoundary {
    void prepareSuccessView(CreateTeamOutputData outputData);

    void prepareFailView(String errorMessage);
}
