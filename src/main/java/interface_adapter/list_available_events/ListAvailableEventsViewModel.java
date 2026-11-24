package interface_adapter.list_available_events;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import use_case.list_available_events.AvailableEvent;
public final class ListAvailableEventsViewModel {
 private final PropertyChangeSupport support = new PropertyChangeSupport(this);
 private List<AvailableEvent> events = List.of();
 private String error = "";
 public List<AvailableEvent> getEvents() { return events; }
 public String getError() { return error; }
 public void setEvents(List<AvailableEvent> events) {
  this.events = List.copyOf(events); error = "";
  support.firePropertyChange("state", null, this);
 }
 public void setError(String message) {
  events = List.of(); error = message;
  support.firePropertyChange("state", null, this);
 }
 public void addPropertyChangeListener(PropertyChangeListener l) {
  support.addPropertyChangeListener(l);
 }
}
