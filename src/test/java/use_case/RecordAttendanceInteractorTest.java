package use_case;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import entity.*;
import use_case.current_user.CurrentUserInterface;
import use_case.record_attendance.*;

/** Unit tests do not require Swing, disk, or network. */
public class RecordAttendanceInteractorTest {
    private final Team team = new Team("team", "A", "Basketball", "coach");
    private final Event event = new Event("event", "team", Event.EventType.PRACTICE,
            LocalDateTime.of(2026, 11, 10, 12, 0), "Gym", null);
    private final User coach = new User("coach", "pw", "Coach", Role.COACH);
    private final Stub current = new Stub();
    private final Output output = new Output();
    private final Fake store = new Fake();
    public RecordAttendanceInteractorTest() { team.addPlayer("player"); }
    private RecordAttendanceInteractor interactor() { return new RecordAttendanceInteractor(store, output, current); }
    private Map<String, AttendanceRecord.Status> status() {
        return Map.of("player", AttendanceRecord.Status.PRESENT);
    }
    @Test void savesWholeRoster() {
        current.username = "coach";
        interactor().execute(new RecordAttendanceInputData("event", status()));
        assertEquals(1, store.saved.size());
        assertEquals(AttendanceRecord.Status.PRESENT, store.saved.get(0).getStatus());
        assertNull(output.failure);
    }
    @Test void rejectsAnonymousRequest() {
        interactor().execute(new RecordAttendanceInputData("event", status()));
        assertTrue(output.failure.contains("Sign in"));
        assertEquals(0, store.saved.size());
    }
    @Test void rejectsOutsiderPlayer() {
        current.username = "coach";
        interactor().execute(new RecordAttendanceInputData("event",
                Map.of("intruder", AttendanceRecord.Status.PRESENT)));
        assertNotNull(output.failure);
        assertEquals(0, store.saved.size());
    }
    @Test void rejectsWrongRole() {
        current.username = "player";
        interactor().execute(new RecordAttendanceInputData("event", status()));
        assertNotNull(output.failure);
    }
    @Test void propagatesFailedPersistenceToPresenter() {
        current.username = "coach";
        store.failWrite = true;
        interactor().execute(new RecordAttendanceInputData("event", status()));
        assertTrue(output.failure.contains("Could not record"));
    }
    private final class Fake implements RecordAttendanceDataAccessInterface {
        List<AttendanceRecord> saved = List.of();
        boolean failWrite;
        @Override public User get(String name) {
            return "coach".equals(name) ? coach : new User(name, "pw", name, Role.PLAYER);
        }
        @Override public Event getEvent(String id) { return "event".equals(id) ? event : null; }
        @Override public Team getTeam(String id) { return "team".equals(id) ? team : null; }
        @Override public List<AttendanceRecord> getAttendanceFor(String id) { return saved; }
        @Override public void saveAttendanceRoster(String id, List<AttendanceRecord> records) {
            if (failWrite) { throw new IllegalStateException("test write failure"); }
            saved = List.copyOf(records);
        }
    }
    private static final class Stub implements CurrentUserInterface {
        String username = "";
        @Override public String getCurrentUsername() { return username; }
        @Override public void setCurrentUser(User user) { username = user.getUsername(); }
        @Override public void clearCurrentUser() { username = ""; }
    }
    private static final class Output implements RecordAttendanceOutputBoundary {
        String failure;
        @Override public void prepareSuccessView(RecordAttendanceOutputData data) { failure = null; }
        @Override public void prepareRosterView(RecordAttendanceRosterData data) { failure = null; }
        @Override public void prepareFailView(String message) { failure = message; }
    }
}
