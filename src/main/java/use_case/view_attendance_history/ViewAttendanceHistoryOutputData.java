package use_case.view_attendance_history;
import java.util.List;
public final class ViewAttendanceHistoryOutputData {
 private final List<AttendanceHistoryRow> rows;
 public ViewAttendanceHistoryOutputData(List<AttendanceHistoryRow> rows) {
  this.rows = List.copyOf(rows);
 }
 public List<AttendanceHistoryRow> getRows() { return rows; }
}
