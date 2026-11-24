package use_case.database_settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.DatabaseConfig;
import data_access.LegacyKeys;

/**
 * Tests the Database Settings use case: the view it prepares, a check that works,
 * every way the typed values are refused, and a save that works or fails.
 *
 * The data layer is a recording fake here, so the rules and the wording are
 * tested without a network or a file.
 */
class DatabaseSettingsInteractorTest {

    private static final String URL = "https://example.supabase.co";
    private static final String KEY = "sb_publishable_test";

    /** Captures what the presenter is asked to show. */
    private static class RecordingPresenter implements DatabaseSettingsOutputBoundary {

        private String currentUrl;
        private String settingsFile;
        private DatabaseSettingsOutputData result;

        @Override
        public void prepareView(String currentUrl, String settingsFile) {
            this.currentUrl = currentUrl;
            this.settingsFile = settingsFile;
        }

        @Override
        public void prepareResult(DatabaseSettingsOutputData outputData) {
            this.result = outputData;
        }
    }

    /** A store that answers with whatever the test configures. */
    private static class FakeStore implements DatabaseSettingsDataAccessInterface {

        private final List<String> calls;
        private int accounts = 4;
        private RuntimeException failure;
        private RuntimeException saveFailure;

        FakeStore(List<String> calls) {
            this.calls = calls;
        }

        @Override
        public int countAccounts(String url, String key) {
            calls.add("test:" + url + "|" + key);
            if (failure != null) {
                throw failure;
            }
            return accounts;
        }

        @Override
        public void save(String url, String key) {
            calls.add("save:" + url + "|" + key);
            if (saveFailure != null) {
                throw saveFailure;
            }
        }

        @Override
        public boolean isSecretKey(String key) {
            return DatabaseConfig.isSecretKey(key);
        }

        @Override
        public boolean isSecureUrl(String url) {
            return DatabaseConfig.isSecureUrl(url);
        }

        @Override
        public String currentUrl() {
            return URL;
        }

        @Override
        public String settingsFile() {
            return "/home/pat/.team-sports/database.properties";
        }
    }

    private final List<String> calls = new ArrayList<>();

    private RecordingPresenter presenter;
    private FakeStore store;
    private DatabaseSettingsInteractor interactor;

    @BeforeEach
    void setUp() {
        presenter = new RecordingPresenter();
        store = new FakeStore(calls);
        interactor = new DatabaseSettingsInteractor(store, presenter);
    }

    @Test
    void thePageStartsWithTheConfiguredUrlAndTheFileItSavesTo() {
        interactor.prepareView();

        assertEquals(URL, presenter.currentUrl);
        assertTrue(presenter.settingsFile.endsWith("database.properties"));
        assertTrue(calls.isEmpty(), "preparing the view must not touch the database");
    }

    @Test
    void aWorkingConnectionReportsHowManyAccountsItSees() {
        interactor.test(new DatabaseSettingsInputData(URL, KEY));

        assertTrue(presenter.result.isOk());
        assertEquals("Connected: 4 account(s) stored.", presenter.result.getMessage());
        assertEquals(List.of("test:" + URL + "|" + KEY), calls);
    }

    @Test
    void aMissingValueIsRefusedBeforeTheDatabaseIsAsked() {
        interactor.test(new DatabaseSettingsInputData("", KEY));

        assertEquals("Enter both a project URL and a publishable key.",
                presenter.result.getMessage());
        assertTrue(calls.isEmpty(), "nothing should be sent with a blank value");
    }

    @Test
    void aSecretKeyIsRefusedBecauseItWouldBypassRowLevelSecurity() {
        interactor.test(new DatabaseSettingsInputData(URL, "sb_secret_abc123"));

        assertTrue(presenter.result.getMessage().contains("secret key"));
        assertTrue(presenter.result.getMessage().contains("publishable"));
        assertTrue(calls.isEmpty());
    }

    @Test
    void aUrlThatIsNotHttpsIsRefused() {
        interactor.test(new DatabaseSettingsInputData("http://example.supabase.co", KEY));

        assertEquals("The project URL must start with https://", presenter.result.getMessage());
        assertTrue(calls.isEmpty());
    }

    @Test
    void aLegacyServiceRoleKeyIsRefusedHereToo() {
        interactor.test(new DatabaseSettingsInputData(URL, LegacyKeys.of("service_role")));

        assertTrue(presenter.result.getMessage().contains("secret key"),
                presenter.result.getMessage());
        assertTrue(calls.isEmpty(),
                "the page must refuse it before the key is ever sent");
    }

    @Test
    void aLocalHttpUrlIsAllowedBecauseALocalStackNeverLeavesTheMachine() {
        interactor.test(new DatabaseSettingsInputData("http://127.0.0.1:54321", KEY));

        assertTrue(presenter.result.isOk(), presenter.result.getMessage());
        assertEquals(List.of("test:http://127.0.0.1:54321|" + KEY), calls);
    }

    @Test
    void anUnreachableDatabaseIsExplained() {
        store.failure = new UncheckedIOException(new IOException("Connection refused"));

        interactor.test(new DatabaseSettingsInputData(URL, KEY));

        assertTrue(presenter.result.getMessage().startsWith("Could not connect:"),
                presenter.result.getMessage());
        assertTrue(presenter.result.getMessage().contains("Connection refused"));
    }

    @Test
    void aRejectedDatabaseIsExplained() {
        store.failure = new IllegalStateException("Invalid API key");

        interactor.test(new DatabaseSettingsInputData(URL, KEY));

        assertTrue(presenter.result.getMessage().contains("Invalid API key"));
    }

    @Test
    void savingStoresTheValuesAndSaysWhere() {
        interactor.save(new DatabaseSettingsInputData(URL, KEY));

        assertEquals(List.of("save:" + URL + "|" + KEY), calls);
        assertTrue(presenter.result.isOk());
        assertTrue(presenter.result.getMessage().startsWith("Saved to "),
                presenter.result.getMessage());
        assertTrue(presenter.result.getMessage().contains("restart the app"),
                presenter.result.getMessage());
    }

    @Test
    void aFailedSaveIsReported() {
        store.saveFailure = new UncheckedIOException(new IOException("Access denied"));

        interactor.save(new DatabaseSettingsInputData(URL, KEY));

        assertTrue(presenter.result.getMessage().startsWith("Could not save the settings:"),
                presenter.result.getMessage());
        assertTrue(presenter.result.getMessage().contains("Access denied"));
    }
}
