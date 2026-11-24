package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import interface_adapter.database_settings.DatabaseSettingsController;
import interface_adapter.database_settings.DatabaseSettingsViewModel;
import use_case.database_settings.DatabaseSettingsInputBoundary;
import use_case.database_settings.DatabaseSettingsInputData;

/**
 * Tests the Database Settings page: it passes what was typed to the controller,
 * shows what the view model carries, and starts from what the use case reports.
 *
 * The use case is a recording fake, so the page is tested without a network or a
 * file, and the wording of every message is the use case's.
 */
public class DatabaseSettingsViewTest {

    private static final String URL = "https://example.supabase.co";
    private static final String KEY = "sb_publishable_test";

    private final List<String> calls = new ArrayList<>();
    private DatabaseSettingsViewModel viewModel;
    private DatabaseSettingsView view;

    @BeforeEach
    void setUp() {
        final DatabaseSettingsInputBoundary boundary = new DatabaseSettingsInputBoundary() {
            @Override
            public void prepareView() {
                calls.add("prepare");
            }

            @Override
            public void test(DatabaseSettingsInputData inputData) {
                calls.add("test:" + inputData.getUrl() + "|" + inputData.getKey());
            }

            @Override
            public void save(DatabaseSettingsInputData inputData) {
                calls.add("save:" + inputData.getUrl() + "|" + inputData.getKey());
            }
        };

        viewModel = new DatabaseSettingsViewModel();
        view = new DatabaseSettingsView(viewModel);
        view.setController(new DatabaseSettingsController(boundary));
    }

    @Test
    void testingPassesWhatWasTyped() {
        view.typeSettings(URL, KEY);

        view.clickTest();

        assertEquals(List.of("test:" + URL + "|" + KEY), calls);
    }

    @Test
    void savingPassesWhatWasTyped() {
        view.typeSettings(URL, KEY);

        view.clickSave();

        assertEquals(List.of("save:" + URL + "|" + KEY), calls);
    }

    @Test
    void testingSaysItIsWorkingBeforeTheAnswerArrives() {
        view.typeSettings(URL, KEY);

        view.clickTest();

        assertEquals("Testing...", view.status());
    }

    @Test
    void theConfiguredProjectIsFilledIn() {
        viewModel.showSettings(URL, "/home/pat/.team-sports/database.properties");

        assertEquals(URL, view.typedUrl());
    }

    @Test
    void aSuccessfulResultIsShown() {
        viewModel.showResult(true, "Connected: 4 account(s) stored.");

        assertEquals("Connected: 4 account(s) stored.", view.status());
    }

    @Test
    void aFailedResultIsShown() {
        viewModel.showResult(false, "Could not connect: HTTP 401 Invalid API key");

        assertEquals("Could not connect: HTTP 401 Invalid API key", view.status());
    }

    @Test
    void aResultDoesNotOverwriteWhatIsBeingTyped() {
        viewModel.showSettings(URL, "/home/pat/.team-sports/database.properties");
        view.typeSettings("https://other.supabase.co", KEY);

        viewModel.showResult(false, "Could not connect: Connection refused");

        assertEquals("https://other.supabase.co", view.typedUrl(),
                "a message must not replace the URL the user is editing");
    }

    @Test
    void backToLogInCallsTheCallback() {
        final boolean[] called = {false};
        view.setOnBackToLogin(() -> called[0] = true);

        view.clickBackToLogin();

        assertTrue(called[0]);
    }
}
