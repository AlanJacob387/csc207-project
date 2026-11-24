package interface_adapter.flag_rain_risk;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import use_case.flag_rain_risk.FlagRainRiskOutputData;

/** Separates unavailable forecasts from the measured event flags shown on other schedule views. */
public class FlagRainRiskViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private FlagRainRiskOutputData data;
    private String message = "Choose Check rain risk to load upcoming forecasts.";
    private boolean loading;

    public FlagRainRiskOutputData getData() {
        return data;
    }

    public String getMessage() {
        return message;
    }

    public boolean isLoading() {
        return loading;
    }

    public Map<String, String> getFlags() {
        final Map<String, String> flags = new LinkedHashMap<>();
        if (data != null) {
            for (FlagRainRiskOutputData.EventRisk event : data.events()) {
                if (event.rainRisk()) {
                    flags.put(event.eventId(), event.status());
                }
            }
        }
        return Map.copyOf(flags);
    }

    /** Clears the old result while the requested forecast loads. */
    public void showLoading() {
        data = null;
        message = "Checking upcoming weather…";
        loading = true;
        fire();
    }

    /** Replaces the state with a completed authorized result. */
    public void showData(FlagRainRiskOutputData value) {
        data = value;
        message = value.events().isEmpty() ? "No upcoming events are available for your account."
                : "Rain risk is flagged at 60% precipitation probability. Forecasts can change.";
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
        message = "Choose Check rain risk to load upcoming forecasts.";
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
