package use_case.view_attendance_history;
import java.util.List;
import entity.*;
public interface ViewAttendanceHistoryDataAccessInterface {
 User get(String username);
 Event getEvent(String eventId);
 List<AttendanceRecord> getAttendanceForPlayer(String username);
}
