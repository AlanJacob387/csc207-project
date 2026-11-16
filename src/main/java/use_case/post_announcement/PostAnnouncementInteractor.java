package use_case.post_announcement;

import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import entity.Announcement;
import entity.Role;
import entity.Team;
import entity.User;

/** Allows a coach to post only to a team they currently coach. */
public class PostAnnouncementInteractor implements PostAnnouncementInputBoundary {
    private final PostAnnouncementDataAccessInterface dataAccess;
    private final PostAnnouncementOutputBoundary presenter;
    private final Clock clock;

    public PostAnnouncementInteractor(PostAnnouncementDataAccessInterface dataAccess,
                                      PostAnnouncementOutputBoundary presenter) {
        this(dataAccess, presenter, Clock.systemDefaultZone());
    }

    /** Accepts a clock so posting time is reproducible in tests. */
    public PostAnnouncementInteractor(PostAnnouncementDataAccessInterface dataAccess,
                                      PostAnnouncementOutputBoundary presenter, Clock clock) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.clock = clock;
    }

    @Override
    public void execute(PostAnnouncementInputData inputData) {
        if (inputData == null || isBlank(inputData.getUsername())
                || isBlank(inputData.getTeamId()) || isBlank(inputData.getMessage())) {
            presenter.prepareFailView("Choose a team and enter an announcement while signed in.");
            return;
        }

        final String teamId = inputData.getTeamId().trim();
        final Announcement announcement;
        try {
            final User user = dataAccess.get(inputData.getUsername());
            if (user == null || user.getRole() != Role.COACH) {
                presenter.prepareFailView("Only a signed-in coach can post announcements.");
                return;
            }
            final Team team = dataAccess.getTeam(teamId);
            if (team == null) {
                presenter.prepareFailView("The selected team does not exist.");
                return;
            }
            if (!user.getUsername().equals(team.getCoachUsername())) {
                presenter.prepareFailView("You can post announcements only for a team you coach.");
                return;
            }

            announcement = new Announcement(UUID.randomUUID().toString(), teamId,
                    user.getUsername(), inputData.getMessage().trim(), LocalDateTime.now(clock));
            dataAccess.saveAnnouncement(announcement);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the announcement: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(new PostAnnouncementOutputData(
                announcement.getAnnouncementId(), announcement.getTeamId()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
