package interface_adapter.database_settings;

import use_case.database_settings.DatabaseSettingsInputBoundary;
import use_case.database_settings.DatabaseSettingsInputData;

/**
 * Controller for the Database Settings page.
 *
 * It builds the input data from what the user typed and decides nothing: the
 * rules about keys and URLs live in the use case.
 */
public class DatabaseSettingsController {

    private final DatabaseSettingsInputBoundary settings;

    /**
     * Creates the controller.
     *
     * @param settings the Database Settings use case
     */
    public DatabaseSettingsController(DatabaseSettingsInputBoundary settings) {
        this.settings = settings;
    }

    /** Fills the page with the settings that are already configured. */
    public void prepareView() {
        settings.prepareView();
    }

    /**
     * Checks the typed values against the database.
     *
     * @param url the project URL as typed
     * @param key the publishable key as typed
     */
    public void test(String url, String key) {
        settings.test(new DatabaseSettingsInputData(url, key));
    }

    /**
     * Saves the typed values for this user.
     *
     * @param url the project URL as typed
     * @param key the publishable key as typed
     */
    public void save(String url, String key) {
        settings.save(new DatabaseSettingsInputData(url, key));
    }
}
