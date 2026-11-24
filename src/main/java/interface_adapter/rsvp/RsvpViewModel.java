package interface_adapter.rsvp;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
public final class RsvpViewModel {
 private final PropertyChangeSupport support = new PropertyChangeSupport(this);
 private String message = "";
 private List<String> responses = List.of();
 public String getMessage() { return message; }
 public List<String> getResponses() { return responses; }
 public void setState(String message, List<String> rows) {
  this.message = message; this.responses = List.copyOf(rows);
  support.firePropertyChange("state", null, this);
 }
 public void addPropertyChangeListener(PropertyChangeListener l) {
  support.addPropertyChangeListener(l);
 }
}
