package use_case.database_settings;

import java.io.UncheckedIOException;

/**
 * Interactor for the Database Settings use case.
 *
 * It decides when the questions are asked and words every outcome: both values
 * present, never a secret key, and a URL that cannot expose the key. The two
 * decisions themselves come from the data layer - {@code isSecretKey} and
 * {@code isSecureUrl} - so the page, the environment variables and the settings
 * file cannot drift apart, and there is one implementation to change.
 */
public class DatabaseSettingsInteractor implements DatabaseSettingsInputBoundary {

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
    private String refusalFor(DatabaseSettingsInputData inputData) {
        final String url = inputData.getUrl() == null ? "" : inputData.getUrl().trim();
        final String key = inputData.getKey() == null ? "" : inputData.getKey().trim();
        if (url.isEmpty() || key.isEmpty()) {
            return "Enter both a project URL and a publishable key.";
        }
        if (dataAccess.isSecretKey(key)) {
            return "That is a secret key. A secret key bypasses row level security and must not "
                    + "be used in a desktop app; paste the publishable key "
                    + "(sb_publishable_...) instead.";
        }
        if (!dataAccess.isSecureUrl(url)) {
            return "The project URL must start with https://";
        }
        return null;
    }
}
