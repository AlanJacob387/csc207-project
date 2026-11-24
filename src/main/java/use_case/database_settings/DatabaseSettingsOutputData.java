package use_case.database_settings;

/**
 * What the settings page should show after a check or a save: whether it worked,
 * and the sentence that explains it.
 *
 * The sentence is built here rather than in the store or the view, so the wording
 * of a connection problem has one home.
 */
public final class DatabaseSettingsOutputData {

    private final boolean ok;
    private final String message;

    /**
     * Creates the result.
     *
     * @param ok      whether the action worked
     * @param message what to show
     */
    public DatabaseSettingsOutputData(boolean ok, String message) {
        this.ok = ok;
        this.message = message;
    }

    public boolean isOk() {
        return ok;
    }

    public String getMessage() {
        return message;
    }
}
