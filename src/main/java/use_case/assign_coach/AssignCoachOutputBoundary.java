package use_case.assign_coach;

/** Receives the result after assigning a coach is attempted. */
public interface AssignCoachOutputBoundary {
    void prepareSuccessView(AssignCoachOutputData outputData);

    void prepareFailView(String errorMessage);
}
