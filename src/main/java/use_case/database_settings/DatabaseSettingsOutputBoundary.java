package use_case.database_settings;

/**
 * What the settings use case tells the presenter.
 */
public interface DatabaseSettingsOutputBoundary {

    /**
     * Hands the page the settings it should start with.
     *
     * @param currentUrl   the URL already configured, or an empty string
     * @param settingsFile where saving writes, for the hint text
     */
    void prepareView(String currentUrl, String settingsFile);

    /**
     * Reports the outcome of a check or a save.
     *
     * @param outputData whether it worked, and what to show
     */
    void prepareResult(DatabaseSettingsOutputData outputData);
}
