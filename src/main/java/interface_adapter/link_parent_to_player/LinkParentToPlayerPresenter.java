package interface_adapter.link_parent_to_player;

import use_case.link_parent_to_player.LinkParentToPlayerOutputBoundary;
import use_case.link_parent_to_player.LinkParentToPlayerOutputData;

/** Converts a use-case outcome into the observed page state. */
public class LinkParentToPlayerPresenter implements LinkParentToPlayerOutputBoundary {
    private final LinkParentToPlayerViewModel viewModel;

    public LinkParentToPlayerPresenter(LinkParentToPlayerViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(LinkParentToPlayerOutputData data) {
        viewModel.showSuccess(data);
    }

    @Override
    public void prepareFailView(String message) {
        viewModel.showError(message);
    }
}
