package interface_adapter.view_announcements;

import use_case.view_announcements.ViewAnnouncementsOutputBoundary;
import use_case.view_announcements.ViewAnnouncementsOutputData;

/** Presents only the teams and posts that the reading use case authorized. */
public class ViewAnnouncementsPresenter implements ViewAnnouncementsOutputBoundary {
    private final ViewAnnouncementsViewModel viewModel;

    /** Creates a presenter for the team announcement list. */
    public ViewAnnouncementsPresenter(ViewAnnouncementsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ViewAnnouncementsOutputData outputData) {
        viewModel.showAnnouncements(outputData);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.showError(errorMessage);
    }
}
