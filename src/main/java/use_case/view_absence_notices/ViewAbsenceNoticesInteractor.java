package use_case.view_absence_notices;

import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import entity.AbsenceNotice;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.current_user.CurrentUserInterface;
import use_case.view_absence_notices.ViewAbsenceNoticesOutputData.ChildRow;
import use_case.view_absence_notices.ViewAbsenceNoticesOutputData.EventRow;
import use_case.view_absence_notices.ViewAbsenceNoticesOutputData.NoticeRow;

/** Provides a parent's linked-child form or a coach's team-scoped notice list. */
public class ViewAbsenceNoticesInteractor implements ViewAbsenceNoticesInputBoundary {
    private final ViewAbsenceNoticesDataAccessInterface dataAccess;
    private final ViewAbsenceNoticesOutputBoundary presenter;
    private final CurrentUserInterface currentUser;
    private final Clock clock;

    public ViewAbsenceNoticesInteractor(ViewAbsenceNoticesDataAccessInterface dataAccess,
                                        ViewAbsenceNoticesOutputBoundary presenter,
                                        CurrentUserInterface currentUser) {
        this(dataAccess, presenter, currentUser, Clock.systemDefaultZone());
    }

    public ViewAbsenceNoticesInteractor(ViewAbsenceNoticesDataAccessInterface dataAccess,
                                        ViewAbsenceNoticesOutputBoundary presenter,
                                        CurrentUserInterface currentUser, Clock clock) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    public void execute(ViewAbsenceNoticesInputData input) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in to view absence notices.");
            return;
        }
        final ViewAbsenceNoticesOutputData result;
        try {
            final User reader = dataAccess.get(username);
            if (reader == null || reader.getRole() != Role.PARENT && reader.getRole() != Role.COACH) {
                presenter.prepareFailView("Absence notices are available to parents and coaches.");
                return;
            }
            final boolean parent = reader.getRole() == Role.PARENT;
            final List<ChildRow> children = parent ? linkedChildren(username) : List.of();
            final String requested = input == null ? null : input.playerUsername();
            final String selected = requested == null || requested.isBlank()
                    ? children.isEmpty() ? null : children.get(0).username() : requested;
            if (parent && selected != null && children.stream().noneMatch(child -> child.username().equals(selected))) {
                presenter.prepareFailView("Choose a player linked to your parent account.");
                return;
            }
            final Map<String, Team> teams = new LinkedHashMap<>();
            for (Team team : dataAccess.getAllTeams()) {
                if (parent && selected != null && team.getPlayerUsernames().contains(selected)
                        || !parent && username.equals(team.getCoachUsername())) {
                    teams.put(team.getTeamId(), team);
                }
            }
            final Map<String, Event> events = new LinkedHashMap<>();
            final List<EventRow> choices = new ArrayList<>();
            final LocalDateTime now = LocalDateTime.now(clock);
            for (Event event : dataAccess.getAllEvents()) {
                final Team team = teams.get(event.getTeamId());
                if (team != null) {
                    events.put(event.getEventId(), event);
                    if (parent && !event.isCancelled() && event.getDateTime() != null
                            && event.getDateTime().isAfter(now)) {
                        choices.add(new EventRow(event.getEventId(), team.getName(), event.getType().name(),
                                event.getDateTime(), event.getLocation()));
                    }
                }
            }
            choices.sort(Comparator.comparing(EventRow::dateTime).thenComparing(EventRow::eventId));
            final List<NoticeRow> notices = new ArrayList<>();
            for (AbsenceNotice notice : dataAccess.getAbsenceNotices()) {
                final Event event = events.get(notice.getEventId());
                if (event == null || parent && (!username.equals(notice.getParentUsername())
                        || !notice.getPlayerUsername().equals(selected))) {
                    continue;
                }
                final User child = dataAccess.get(notice.getPlayerUsername());
                final User author = dataAccess.get(notice.getParentUsername());
                notices.add(new NoticeRow(notice.getNoticeId(), notice.getParentUsername(),
                        author == null ? notice.getParentUsername() : author.getDisplayName(),
                        notice.getPlayerUsername(), child == null ? notice.getPlayerUsername() : child.getDisplayName(),
                        event.getEventId(), teams.get(event.getTeamId()).getName(), event.getType().name(),
                        event.getDateTime(), event.getLocation(), notice.getReason(), notice.getSubmittedAt()));
            }
            notices.sort(Comparator.comparing(NoticeRow::submittedAt).reversed().thenComparing(NoticeRow::noticeId));
            result = new ViewAbsenceNoticesOutputData(children, parent ? selected : null, choices, notices, parent);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not load absence notices: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(result);
    }

    private List<ChildRow> linkedChildren(String username) {
        final List<ChildRow> children = new ArrayList<>();
        for (String playerUsername : dataAccess.getLinkedPlayerUsernames(username)) {
            final User player = dataAccess.get(playerUsername);
            if (player != null && player.getRole() == Role.PLAYER) {
                children.add(new ChildRow(player.getUsername(), player.getDisplayName()));
            }
        }
        children.sort(Comparator.comparing(ChildRow::displayName).thenComparing(ChildRow::username));
        return children;
    }
}
