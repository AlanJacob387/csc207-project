package use_case.edit_attendance;
import entity.*;
public interface EditAttendanceDataAccessInterface {
 User get(String username);
 java.util.List<Event> getAllEvents();
 Event getEvent(String eventId);
 Team getTeam(String teamId);
 AttendanceRecord getAttendanceRecord(String eventId, String username);
 void updateAttendanceRecord(AttendanceRecord record);
}
