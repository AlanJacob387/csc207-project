package interface_adapter.record_attendance;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import entity.AttendanceRecord;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
/** Observable result of an attendance save. */
public final class RecordAttendanceViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private String message = "";
    private boolean success;
    private Map<String, AttendanceRecord.Status> roster = Map.of();
    private Map<String, String> notes = Map.of();
    public String getMessage() { return message; }
    public boolean isSuccess() { return success; }
    public Map<String, AttendanceRecord.Status> getRoster() { return roster; }
    public Map<String, String> getNotes() { return notes; }
    public void setRoster(Map<String, AttendanceRecord.Status> roster) {
        setRoster(roster, Map.of());
    }
    public void setRoster(Map<String, AttendanceRecord.Status> roster,
            Map<String, String> notes) {
        this.roster = Collections.unmodifiableMap(new LinkedHashMap<>(roster));
        this.notes = Collections.unmodifiableMap(new LinkedHashMap<>(notes));
        message = "Roster loaded.";
        support.firePropertyChange("roster", null, this);
    }
    public void setResult(String message, boolean success) {
        this.message = message;
        this.success = success;
        support.firePropertyChange("state", null, this);
    }
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
