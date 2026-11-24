package interface_adapter.view_event_weather;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import use_case.view_event_weather.ViewEventWeatherOutputData;

/** Observable state of one requested event, never retained across identities or events. */
public class ViewEventWeatherViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private ViewEventWeatherOutputData data;
    private String message = "Choose an event to view weather.";
    private boolean loading;

    public ViewEventWeatherOutputData getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public boolean isLoading() {
        return loading;
    }

    /** Clears the old result while the requested forecast loads. */
    public void showLoading() {
        data = null;
        message = "Loading weather…";
        loading = true;
        fire();
    }

    /** Replaces the state with a completed authorized result. */
    public void showData(ViewEventWeatherOutputData value) {
        data = value;
        message = value.message();
        loading = false;
        fire();
    }

    /** Removes stale content when a request cannot be completed. */
    public void showError(String value) {
        data = null;
        message = value;
        loading = false;
        fire();
    }

    /** Removes all state belonging to the previous request or account. */
    public void clear() {
        data = null;
        message = "Choose an event to view weather.";
        loading = false;
        fire();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    private void fire() {
        support.firePropertyChange("state", null, this);
    }
}
