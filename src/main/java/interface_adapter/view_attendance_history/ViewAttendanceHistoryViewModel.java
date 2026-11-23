package interface_adapter.view_attendance_history;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import use_case.view_attendance_history.AttendanceHistoryRow;
public final class ViewAttendanceHistoryViewModel {
 private final PropertyChangeSupport support = new PropertyChangeSupport(this);
 private List<AttendanceHistoryRow> rows = List.of();
 private String error = "";
 public List<AttendanceHistoryRow> getRows() { return rows; }
 public String getError() { return error; }
 public void setRows(List<AttendanceHistoryRow> rows) {
  this.rows = List.copyOf(rows); this.error = "";
  support.firePropertyChange("state", null, this);
 }
 public void setError(String error) {
  this.rows = List.of(); this.error = error;
  support.firePropertyChange("state", null, this);
 }
 public void clear() { setRows(List.of()); }
 public void addPropertyChangeListener(PropertyChangeListener listener) {
  support.addPropertyChangeListener(listener);
 }
}
