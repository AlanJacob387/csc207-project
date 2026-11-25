package use_case.database_settings;

/**
 * What the settings page can ask for.
 *
 * Two actions, one view: test the values without saving them, or save them for
 * this user. Nothing here names a database, a file or a widget.
 */
public interface DatabaseSettingsInputBoundary {

    /** Fills the page with the settings that are already configured. */
    void prepareView();

    /**
     * Checks the typed values against the database.
     *
     * @param inputData the URL and key as typed
     */
    void test(DatabaseSettingsInputData inputData);

    /**
     * Saves the typed values for this user.
     *
     * @param inputData the URL and key as typed
     */
    void save(DatabaseSettingsInputData inputData);
}
