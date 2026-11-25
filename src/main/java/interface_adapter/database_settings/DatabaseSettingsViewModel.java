package interface_adapter.database_settings;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * State of the Database Settings page: the URL to start from, where a save goes,
 * and the last outcome.
 *
 * The success flag exists so the view can colour the message without deciding
 * whether something worked.
 */
public class DatabaseSettingsViewModel {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String currentUrl = "";
    private String settingsFile = "";
    private String message = "";
    private boolean success;

    /** The URL already configured, or an empty string. */
    public String getCurrentUrl() {
        return currentUrl;
    }

    /** Where a save writes, for the hint text. */
    public String getSettingsFile() {
        return settingsFile;
    }

    public String getMessage() {
        return message;
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * Fills what the page starts with.
     *
     * @param currentUrl   the URL already configured
     * @param settingsFile where a save writes
     */
    public void showSettings(String currentUrl, String settingsFile) {
        this.currentUrl = currentUrl == null ? "" : currentUrl;
        this.settingsFile = settingsFile == null ? "" : settingsFile;
        support.firePropertyChange("settings", null, this);
    }

    /**
     * Shows the outcome of a check or a save.
     *
     * @param ok      whether it worked
     * @param message what to show
     */
    public void showResult(boolean ok, String message) {
        this.success = ok;
        this.message = message == null ? "" : message;
        support.firePropertyChange("result", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
