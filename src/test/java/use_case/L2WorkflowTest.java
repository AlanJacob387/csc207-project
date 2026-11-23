package use_case;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import entity.*;
import use_case.current_user.CurrentUserInterface;
import use_case.edit_attendance.*;
import use_case.rsvp.*;
import use_case.view_attendance_history.*;
import use_case.view_stats.*;

/** Fast tests using a fake storage port; no Swing or networking. */
public final class L2WorkflowTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);
    private final Team team = new Team("t", "Team", "Basketball", "coach");
    private final Event event = new Event("e", "t", Event.EventType.PRACTICE,
            LocalDateTime.of(2026, 12, 1, 10, 0), "Gym", null);
    private final Current current = new Current();
    private final Fake fake = new Fake();
    private final EditOutput editOutput = new EditOutput();
    private final RsvpOutput rsvpOutput = new RsvpOutput();
    private final HistoryOutput historyOutput = new HistoryOutput();
    private final StatsOutput statsOutput = new StatsOutput();

    public L2WorkflowTest() { team.addPlayer("p"); }

    @Test void editsExistingAttendanceButPreservesNotes() {
        fake.attendance.add(withNotes("two baskets"));
        current.username = "coach";
        new EditAttendanceInteractor(fake, editOutput, current).execute(
                new EditAttendanceInputData("e", "p", AttendanceRecord.Status.ABSENT));
        assertNull(editOutput.error);
        assertEquals(AttendanceRecord.Status.ABSENT, fake.attendance.get(0).getStatus());
        assertEquals("two baskets", fake.attendance.get(0).getStatsNotes());
    }

    @Test void clearingNotesInARecordedAttendanceCorrection() {
        fake.attendance.add(withNotes("Old note"));
        current.username = "coach";
        new EditAttendanceInteractor(fake, editOutput, current).execute(
                new EditAttendanceInputData("e", "p", AttendanceRecord.Status.PRESENT, ""));
        assertNull(editOutput.error);
        assertEquals("", fake.attendance.get(0).getStatsNotes());
    }

    @Test void rejectsNonCoachEdit() {
        fake.attendance.add(withNotes(""));
        current.username = "p";
        new EditAttendanceInteractor(fake, editOutput, current).execute(
                new EditAttendanceInputData("e", "p", AttendanceRecord.Status.ABSENT));
        assertNotNull(editOutput.error);
        assertEquals(AttendanceRecord.Status.PRESENT, fake.attendance.get(0).getStatus());
    }

    @Test void playerCanRsvpAndChangeResponse() {
        current.username = "p";
        RsvpInteractor interactor = new RsvpInteractor(fake, rsvpOutput, current, CLOCK);
        interactor.execute(new RsvpInputData("e", RsvpRecord.Response.YES));
        interactor.execute(new RsvpInputData("e", RsvpRecord.Response.NO));
        assertNull(rsvpOutput.error);
        assertEquals(1, fake.responses.size());
        assertEquals(RsvpRecord.Response.NO, fake.responses.get(0).getResponse());
    }

    @Test void playerCannotSubmitLegacyMaybeResponse() {
        current.username = "p";
        new RsvpInteractor(fake, rsvpOutput, current, CLOCK).execute(
                new RsvpInputData("e", RsvpRecord.Response.MAYBE));
        assertNotNull(rsvpOutput.error);
        assertTrue(fake.responses.isEmpty());
    }

    @Test void coachCanSeeResponsesButOtherPlayerCannot() {
        fake.responses.add(new RsvpRecord("e", "p", RsvpRecord.Response.MAYBE));
        RsvpInteractor interactor = new RsvpInteractor(fake, rsvpOutput, current, CLOCK);
        current.username = "coach";
        interactor.execute(new RsvpInputData("e", null));
        assertEquals(List.of("p: MAYBE"), rsvpOutput.data.getResponses());
        current.username = "p";
        interactor.execute(new RsvpInputData("e", null));
        assertNotNull(rsvpOutput.error);
    }

    @Test void playerCannotRsvpToPastEvent() {
        current.username = "p";
        final Event earlier = new Event("past", "t", Event.EventType.PRACTICE,
                LocalDateTime.of(2026, 10, 1, 10, 0), "Gym", null);
        fake.oldEvent = earlier;
        new RsvpInteractor(fake, rsvpOutput, current, CLOCK).execute(
                new RsvpInputData("past", RsvpRecord.Response.YES));
        assertNotNull(rsvpOutput.error);
        assertTrue(fake.responses.isEmpty());
    }

    @Test void historyDoesNotLeakAnotherPlayerData() {
        fake.attendance.add(withNotes("notes"));
        fake.attendance.add(new AttendanceRecord("e", "other", AttendanceRecord.Status.ABSENT));
        current.username = "p";
        new ViewAttendanceHistoryInteractor(fake, historyOutput, current).execute(
                new ViewAttendanceHistoryInputData());
        assertNull(historyOutput.error);
        assertEquals(1, historyOutput.data.getRows().size());
        assertEquals("notes", historyOutput.data.getRows().get(0).getNotes());
    }

    @Test void statsExcludeExcusedFromDenominator() {
        fake.attendance.add(withNotes(""));
        fake.attendance.add(new AttendanceRecord("e2", "p", AttendanceRecord.Status.EXCUSED));
        current.username = "p";
        new ViewStatsInteractor(fake, statsOutput, current).execute(new ViewStatsInputData());
        assertEquals(1.0, statsOutput.data.getAttendanceRate(), 0.0001);
        assertEquals(1, statsOutput.data.getExcused());
    }

    private AttendanceRecord withNotes(String notes) {
        AttendanceRecord record = new AttendanceRecord("e", "p", AttendanceRecord.Status.PRESENT);
        record.setStatsNotes(notes);
        return record;
    }

    private final class Fake implements EditAttendanceDataAccessInterface, RsvpDataAccessInterface,
            ViewAttendanceHistoryDataAccessInterface, ViewStatsDataAccessInterface {
        List<AttendanceRecord> attendance = new ArrayList<>();
        List<RsvpRecord> responses = new ArrayList<>();
        Event oldEvent;
        @Override public User get(String username) {
            if ("coach".equals(username)) { return new User("coach", "pw", "Coach", Role.COACH); }
            if ("p".equals(username)) { return new User("p", "pw", "Player", Role.PLAYER); }
            return null;
        }
        @Override public List<Event> getAllEvents() { return List.of(event); }
        @Override public Event getEvent(String id) {
            if ("e".equals(id)) { return event; }
            if ("past".equals(id)) { return oldEvent; }
            if ("e2".equals(id)) { return event; }
            return null;
        }
        @Override public Team getTeam(String id) { return "t".equals(id) ? team : null; }
        @Override public AttendanceRecord getAttendanceRecord(String eid, String player) {
            return attendance.stream().filter(r -> r.getEventId().equals(eid)
                    && r.getPlayerUsername().equals(player)).findFirst().orElse(null);
        }
        @Override public void updateAttendanceRecord(AttendanceRecord record) {
            attendance.removeIf(r -> r.getEventId().equals(record.getEventId())
                    && r.getPlayerUsername().equals(record.getPlayerUsername()));
            attendance.add(record);
        }
        @Override public List<AttendanceRecord> getAttendanceForPlayer(String username) {
            return attendance.stream().filter(r -> r.getPlayerUsername().equals(username)).toList();
        }
        @Override public void saveRsvp(RsvpRecord record) {
            responses.removeIf(r -> r.getEventId().equals(record.getEventId())
                    && r.getPlayerUsername().equals(record.getPlayerUsername()));
            responses.add(record);
        }
        @Override public List<RsvpRecord> getRsvpsForEvent(String id) {
            return responses.stream().filter(r -> r.getEventId().equals(id)).toList();
        }
    }

    private static final class Current implements CurrentUserInterface {
        String username = "";
        @Override public String getCurrentUsername() { return username; }
        @Override public void setCurrentUser(User user) { username = user.getUsername(); }
        @Override public void clearCurrentUser() { username = ""; }
    }
    private static final class EditOutput implements EditAttendanceOutputBoundary {
        String error;
        @Override public void prepareSuccessView(EditAttendanceOutputData data) { error = null; }
        @Override public void prepareFailView(String error) { this.error = error; }
    }
    private static final class RsvpOutput implements RsvpOutputBoundary {
        String error;
        RsvpOutputData data;
        @Override public void prepareSuccessView(RsvpOutputData output) { error = null; data = output; }
        @Override public void prepareFailView(String error) { this.error = error; }
    }
    private static final class HistoryOutput implements ViewAttendanceHistoryOutputBoundary {
        String error;
        ViewAttendanceHistoryOutputData data;
        @Override public void prepareSuccessView(ViewAttendanceHistoryOutputData output) {
            error = null; data = output;
        }
        @Override public void prepareFailView(String error) { this.error = error; }
    }
    private static final class StatsOutput implements ViewStatsOutputBoundary {
        ViewStatsOutputData data;
        @Override public void prepareSuccessView(ViewStatsOutputData output) { data = output; }
        @Override public void prepareFailView(String error) { data = null; }
    }
}
