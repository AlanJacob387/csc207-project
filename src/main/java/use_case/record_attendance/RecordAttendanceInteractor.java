package use_case.record_attendance;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import entity.AttendanceRecord;
import entity.Event;
import entity.Team;
import entity.User;
import entity.Role;
import use_case.current_user.CurrentUserInterface;

/** Validates a coach's complete roster before committing it as a single write. */
public final class RecordAttendanceInteractor implements RecordAttendanceInputBoundary {
    private final RecordAttendanceDataAccessInterface dataAccess;
    private final RecordAttendanceOutputBoundary presenter;
    private final CurrentUserInterface currentUser;
    public RecordAttendanceInteractor(RecordAttendanceDataAccessInterface dataAccess,
            RecordAttendanceOutputBoundary presenter, CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }
    @Override
    public void execute(RecordAttendanceInputData data) {
        if (data == null || data.getEventId() == null || data.getEventId().isBlank()
                || data.getStatuses() == null) {
            presenter.prepareFailView("Select an event and mark every player.");
            return;
        }
        if (currentUser.getCurrentUsername() == null || currentUser.getCurrentUsername().isBlank()) {
            presenter.prepareFailView("Sign in as a coach to record attendance.");
            return;
        }
        try {
            final User actor = dataAccess.get(currentUser.getCurrentUsername());
            if (actor == null || actor.getRole() != Role.COACH) {
                presenter.prepareFailView("Only a coach can record attendance.");
                return;
            }
            final Event event = dataAccess.getEvent(data.getEventId());
            if (event == null) {
                presenter.prepareFailView("Event not found.");
                return;
            }
            if (event.isCancelled()) {
                presenter.prepareFailView("Cannot record attendance for a cancelled event.");
                return;
            }
            final Team team = dataAccess.getTeam(event.getTeamId());
            if (team == null || !currentUser.getCurrentUsername().equals(team.getCoachUsername())) {
                presenter.prepareFailView("Only the event team's coach can record attendance.");
                return;
            }
            final List<String> players = team.getPlayerUsernames();
            final Map<String, AttendanceRecord.Status> statuses = data.getStatuses();
            final Set<String> expected = new HashSet<>(players);
            if (expected.size() != players.size() || !statuses.keySet().equals(expected)) {
                presenter.prepareFailView("Mark each roster player exactly once, and no outsiders.");
                return;
            }
            final List<AttendanceRecord> records = new ArrayList<>();
            final Map<String, AttendanceRecord> oldRecords = new java.util.HashMap<>();
            for (AttendanceRecord old : dataAccess.getAttendanceFor(event.getEventId())) {
                oldRecords.put(old.getPlayerUsername(), old);
            }
            for (String player : players) {
                final AttendanceRecord.Status status = statuses.get(player);
                if (status == null) {
                    presenter.prepareFailView("Attendance status is missing for " + player + ".");
                    return;
                }
                final AttendanceRecord record = new AttendanceRecord(event.getEventId(), player, status);
                // Preserve existing performance notes when re-recording a roster.
                final AttendanceRecord old = oldRecords.get(player);
                if (old != null) { record.setStatsNotes(old.getStatsNotes()); }
                records.add(record);
            }
            dataAccess.saveAttendanceRoster(event.getEventId(), records);
            presenter.prepareSuccessView(new RecordAttendanceOutputData(event.getEventId(), records.size()));
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not record attendance: " + ex.getMessage());
        }
    }
}
