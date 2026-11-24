package interface_adapter.link_parent_to_player;

import use_case.link_parent_to_player.LinkParentToPlayerInputBoundary;
import use_case.link_parent_to_player.LinkParentToPlayerInputData;

/** Passes form input to the use case, which reads the current session itself. */
public class LinkParentToPlayerController {
    private final LinkParentToPlayerInputBoundary interactor;

    public LinkParentToPlayerController(LinkParentToPlayerInputBoundary interactor) {
        this.interactor = interactor;
    }

    public void execute(String action, String parentUsername, String playerUsername) {
        interactor.execute(new LinkParentToPlayerInputData(action, parentUsername, playerUsername));
    }
}
