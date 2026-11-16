package use_case.assign_coach;

/** Accepts a request to set a team's coach. */
public interface AssignCoachInputBoundary {
    void execute(AssignCoachInputData inputData);
}
