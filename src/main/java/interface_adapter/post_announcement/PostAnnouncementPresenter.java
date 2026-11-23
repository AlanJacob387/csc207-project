package interface_adapter.post_announcement;

import use_case.post_announcement.PostAnnouncementOutputBoundary;
import use_case.post_announcement.PostAnnouncementOutputData;

/** Updates posting feedback after validation and persistence have finished. */
public class PostAnnouncementPresenter implements PostAnnouncementOutputBoundary {
    private final PostAnnouncementViewModel viewModel;

    /** Creates a presenter for the announcement composer. */
    public PostAnnouncementPresenter(PostAnnouncementViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(PostAnnouncementOutputData outputData) {
        viewModel.showSuccess(outputData.getTeamId());
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
