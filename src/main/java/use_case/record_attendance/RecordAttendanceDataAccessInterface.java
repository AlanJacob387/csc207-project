package use_case.record_attendance;
import java.util.List;
import entity.AttendanceRecord;
import entity.Event;
import entity.Team;
import entity.User;
/** Required storage operations, kept separate from the concrete JSON DAO. */
public interface RecordAttendanceDataAccessInterface {
    User get(String username);
    Event getEvent(String eventId);
    Team getTeam(String teamId);
    List<AttendanceRecord> getAttendanceFor(String eventId);
    void saveAttendanceRoster(String eventId, List<AttendanceRecord> records);
}
