package use_case.link_parent_to_player;

/** Receives an authorized result or an explanation of why the request failed. */
public interface LinkParentToPlayerOutputBoundary {
    void prepareSuccessView(LinkParentToPlayerOutputData outputData);
    void prepareFailView(String errorMessage);
}
