package use_case.submit_absence_notice;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import entity.AbsenceNotice;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/** Checks current parent authorization, event status, and roster before saving. */
public class SubmitAbsenceNoticeInteractor implements SubmitAbsenceNoticeInputBoundary {
    private final SubmitAbsenceNoticeDataAccessInterface dataAccess;
    private final SubmitAbsenceNoticeOutputBoundary presenter;
    private final CurrentUserInterface currentUser;
    private final Clock clock;

    public SubmitAbsenceNoticeInteractor(SubmitAbsenceNoticeDataAccessInterface dataAccess,
                                         SubmitAbsenceNoticeOutputBoundary presenter,
                                         CurrentUserInterface currentUser) {
        this(dataAccess, presenter, currentUser, Clock.systemDefaultZone());
    }

    public SubmitAbsenceNoticeInteractor(SubmitAbsenceNoticeDataAccessInterface dataAccess,
                                         SubmitAbsenceNoticeOutputBoundary presenter,
                                         CurrentUserInterface currentUser, Clock clock) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public void execute(SubmitAbsenceNoticeInputData input) {
        if (input == null || isBlank(input.playerUsername()) || isBlank(input.eventId())
                || isBlank(input.reason())) {
            presenter.prepareFailView("Choose a child and event, and enter an absence reason.");
            return;
        }
        final String username = currentUser.getCurrentUsername();
        if (isBlank(username)) {
            presenter.prepareFailView("Sign in as a parent to report an absence.");
            return;
        }
        final String playerUsername = input.playerUsername();
        final String eventId = input.eventId();
        final AbsenceNotice notice;
        try {
            final User parent = dataAccess.get(username);
            final User player = dataAccess.get(playerUsername);
            if (parent == null || parent.getRole() != Role.PARENT) {
                presenter.prepareFailView("Only a parent can submit an absence notice.");
                return;
            }
            if (player == null || player.getRole() != Role.PLAYER
                    || !dataAccess.getLinkedPlayerUsernames(username).contains(playerUsername)) {
                presenter.prepareFailView("Choose a player linked to your parent account.");
                return;
            }
            final Event event = dataAccess.getEvent(eventId);
            if (event == null || event.isCancelled() || event.getDateTime() == null
                    || !event.getDateTime().isAfter(LocalDateTime.now(clock))) {
                presenter.prepareFailView("Absence notices require an upcoming, active event.");
                return;
            }
            final Team team = dataAccess.getTeam(event.getTeamId());
            if (team == null || !team.getPlayerUsernames().contains(playerUsername)) {
                presenter.prepareFailView("This event does not belong to your child's team.");
                return;
            }
            // The same parent/child/event is one notice. Resubmitting updates its reason.
            final String identity = username.length() + ":" + username
                    + playerUsername.length() + ":" + playerUsername + eventId;
            final String noticeId = UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
            notice = new AbsenceNotice(noticeId, username, playerUsername, eventId,
                    input.reason().trim(), LocalDateTime.now(clock));
            dataAccess.saveAbsenceNotice(notice);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the absence notice: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(new SubmitAbsenceNoticeOutputData(notice.getNoticeId(),
                "Absence notice saved. Submitting again for this event updates your reason."));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
