package use_case.view_stats;
import java.util.List;
import entity.*;
public interface ViewStatsDataAccessInterface {
 User get(String username);
 List<AttendanceRecord> getAttendanceForPlayer(String username);
}
