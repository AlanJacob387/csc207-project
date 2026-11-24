package use_case.database_settings;

/**
 * What the settings page sends when the user presses a button: the two values
 * exactly as typed.
 */
public final class DatabaseSettingsInputData {

    private final String url;
    private final String key;

    /**
     * Creates the input data.
     *
     * @param url the project URL, as typed
     * @param key the publishable key, as typed
     */
    public DatabaseSettingsInputData(String url, String key) {
        this.url = url;
        this.key = key;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }
}
