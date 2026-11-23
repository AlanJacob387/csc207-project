package interface_adapter.edit_attendance;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
public final class EditAttendanceViewModel {
 private final PropertyChangeSupport support = new PropertyChangeSupport(this);
 private String message = "";
 private boolean success;
 public String getMessage() { return message; }
 public boolean isSuccess() { return success; }
 public void setResult(String message, boolean success) {
  this.message = message;
  this.success = success;
  support.firePropertyChange("state", null, this);
 }
 public void addPropertyChangeListener(PropertyChangeListener listener) {
  support.addPropertyChangeListener(listener);
 }
}
