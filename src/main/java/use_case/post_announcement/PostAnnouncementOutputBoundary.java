package use_case.post_announcement;

/** Receives the result after posting an announcement is attempted. */
public interface PostAnnouncementOutputBoundary {
    void prepareSuccessView(PostAnnouncementOutputData outputData);

    void prepareFailView(String errorMessage);
}
