package use_case.link_parent_to_player;

/** Accepts a request for this parent-access use case. */
public interface LinkParentToPlayerInputBoundary {
    void execute(LinkParentToPlayerInputData inputData);
}
