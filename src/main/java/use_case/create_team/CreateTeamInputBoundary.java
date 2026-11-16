package use_case.create_team;

/** Accepts a request to create a new team. */
public interface CreateTeamInputBoundary {
    void execute(CreateTeamInputData inputData);
}
