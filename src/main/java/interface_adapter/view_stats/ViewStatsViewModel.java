package interface_adapter.view_stats;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import use_case.view_stats.ViewStatsOutputData;
public final class ViewStatsViewModel {
 private final PropertyChangeSupport support = new PropertyChangeSupport(this);
 private ViewStatsOutputData data = new ViewStatsOutputData(0, 0, 0);
 private String error = "";
 public ViewStatsOutputData getData() { return data; }
 public String getError() { return error; }
 public void setData(ViewStatsOutputData data) {
  this.data = data; this.error = "";
  support.firePropertyChange("state", null, this);
 }
 public void setError(String error) {
  this.data = new ViewStatsOutputData(0, 0, 0); this.error = error;
  support.firePropertyChange("state", null, this);
 }
 public void clear() { setData(new ViewStatsOutputData(0, 0, 0)); }
 public void addPropertyChangeListener(PropertyChangeListener listener) {
  support.addPropertyChangeListener(listener);
 }
}
