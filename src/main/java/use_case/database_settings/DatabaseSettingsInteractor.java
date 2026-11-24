package use_case.database_settings;

import java.io.UncheckedIOException;

/**
 * Interactor for the Database Settings use case.
 *
 * It owns the rules for a pair of settings - both values present, a URL that
 * starts with https, and never the secret key, which would bypass row level
 * security - and it owns the wording of every outcome. The data layer is asked
 * only for facts: how many accounts these settings can see, and whether a save
 * worked.
 *
 * The same checks exist inside {@code DatabaseConfig} as a backstop for the other
 * way in (environment variables and system properties); this is where a person
 * sees them.
 */
public class DatabaseSettingsInteractor implements DatabaseSettingsInputBoundary {

    private static final String SECRET_PREFIX = "sb_secret_";

    private final DatabaseSettingsDataAccessInterface dataAccess;
    private final DatabaseSettingsOutputBoundary presenter;

    /**
     * Creates the interactor.
     *
     * @param dataAccess where the settings are checked and stored
     * @param presenter  where the result is sent
     */
    public DatabaseSettingsInteractor(DatabaseSettingsDataAccessInterface dataAccess,
                                      DatabaseSettingsOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void prepareView() {
        presenter.prepareView(dataAccess.currentUrl(), dataAccess.settingsFile());
    }

    @Override
    public void test(DatabaseSettingsInputData inputData) {
        final String refusal = refusalFor(inputData);
        if (refusal != null) {
            presenter.prepareResult(new DatabaseSettingsOutputData(false, refusal));
            return;
        }
        try {
            final int accounts = dataAccess.countAccounts(inputData.getUrl(),
                    inputData.getKey());
            presenter.prepareResult(new DatabaseSettingsOutputData(true,
                    "Connected: " + accounts + " account(s) stored."));
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareResult(new DatabaseSettingsOutputData(false,
                    "Could not connect: " + ex.getMessage()));
        }
    }

    @Override
    public void save(DatabaseSettingsInputData inputData) {
        final String refusal = refusalFor(inputData);
        if (refusal != null) {
            presenter.prepareResult(new DatabaseSettingsOutputData(false, refusal));
            return;
        }
        try {
            dataAccess.save(inputData.getUrl(), inputData.getKey());
            presenter.prepareResult(new DatabaseSettingsOutputData(true,
                    "Saved to " + dataAccess.settingsFile() + " - restart the app to connect."));
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareResult(new DatabaseSettingsOutputData(false,
                    "Could not save the settings: " + ex.getMessage()));
        }
    }

    /**
     * Checks the rules that do not need a network.
     *
     * @param inputData the typed values
     * @return the sentence to show, or null when the values may be tried
     */
    private static String refusalFor(DatabaseSettingsInputData inputData) {
        final String url = inputData.getUrl() == null ? "" : inputData.getUrl().trim();
        final String key = inputData.getKey() == null ? "" : inputData.getKey().trim();
        if (url.isEmpty() || key.isEmpty()) {
            return "Enter both a project URL and a publishable key.";
        }
        if (key.startsWith(SECRET_PREFIX)) {
            return "That is a secret key. A secret key bypasses row level security and must not "
                    + "be used in a desktop app; paste the publishable key "
                    + "(sb_publishable_...) instead.";
        }
        if (!url.startsWith("https://")) {
            return "The project URL must start with https://";
        }
        return null;
    }
}
