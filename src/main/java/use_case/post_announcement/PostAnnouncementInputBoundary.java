package use_case.post_announcement;

/** Accepts a request to post a message to a team. */
public interface PostAnnouncementInputBoundary {
    void execute(PostAnnouncementInputData inputData);
}
