package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import org.junit.jupiter.api.Test;

/**
 * Tests the database settings page: what it saves, what it refuses, and what it
 * shows after a connection test. The page only collects text, so the saving and
 * testing callbacks are fakes here.
 */
class DatabaseSettingsViewTest {

    private static final String URL = "https://demo.supabase.co";
    private static final String KEY = "sb_publishable_test";
    private static final String FILE = "C:\\Users\\me\\.team-sports\\database.properties";

    @Test
    void savingStoresTheTypedValues() {
        final List<String> saved = new ArrayList<>();
        final DatabaseSettingsView view = new DatabaseSettingsView("", FILE);
        view.setOnSave((url, key) -> saved.add(url + "|" + key));

        view.typeSettings(URL, KEY);
        view.clickSave();

        assertEquals(List.of(URL + "|" + KEY), saved);
        assertTrue(view.status().contains("Saved to"));
        assertTrue(view.status().contains(FILE));
        assertTrue(view.status().contains("restart"));
    }

    @Test
    void savingSomethingIncompleteAsksForBothFields() {
        final List<String> saved = new ArrayList<>();
        final DatabaseSettingsView view = new DatabaseSettingsView("", FILE);
        view.setOnSave((url, key) -> saved.add(url));

        view.typeSettings(URL, "");
        view.clickSave();

        assertTrue(saved.isEmpty(), "nothing should be saved without a key");
        assertTrue(view.status().contains("both a project URL and a publishable key"));
    }

    @Test
    void aSaveFailureIsShownWithItsOwnMessage() {
        final DatabaseSettingsView view = new DatabaseSettingsView("", FILE);
        view.setOnSave((url, key) -> {
            throw new IllegalArgumentException("That is a secret key.");
        });

        view.typeSettings(URL, "sb_secret_abc123");
        view.clickSave();

        assertEquals("That is a secret key.", view.status());
    }

    @Test
    void theConfiguredProjectIsAlreadyFilledIn() {
        final DatabaseSettingsView view = new DatabaseSettingsView(URL, FILE);

        assertEquals(URL, view.typedUrl());
    }

    @Test
    void aSuccessfulConnectionTestShowsItsMessage() {
        final DatabaseSettingsView view = new DatabaseSettingsView(URL, FILE);
        view.setOnTestConnection((url, key) ->
                new DatabaseSettingsView.TestResult(true, "Connected: 4 account(s) stored."));

        view.typeSettings(URL, KEY);
        view.clickTest();

        assertEquals("Connected: 4 account(s) stored.", view.status());
    }

    @Test
    void aFailedConnectionTestShowsItsMessage() {
        final DatabaseSettingsView view = new DatabaseSettingsView(URL, FILE);
        view.setOnTestConnection((url, key) ->
                new DatabaseSettingsView.TestResult(false, "Could not connect: HTTP 401"));

        view.typeSettings(URL, KEY);
        view.clickTest();

        assertEquals("Could not connect: HTTP 401", view.status());
    }

    @Test
    void testingWithoutASecretYetChecksBothFieldsFirst() {
        final DatabaseSettingsView view = new DatabaseSettingsView("", FILE);
        view.setOnTestConnection((url, key) ->
                new DatabaseSettingsView.TestResult(true, "should not be called"));

        view.typeSettings(URL, "");
        view.clickTest();

        assertTrue(view.status().contains("both a project URL and a publishable key"));
    }

    @Test
    void backToLogInCallsTheCallback() {
        final boolean[] called = {false};
        final DatabaseSettingsView view = new DatabaseSettingsView("", FILE);
        view.setOnBackToLogin(() -> called[0] = true);

        final JButton back = findButton(view, "Back to log in");
        assertNotNull(back, "the page should offer a way back to the sign-in page");
        back.doClick();

        assertTrue(called[0]);
    }

    private static JButton findButton(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton && text.equals(((JButton) child).getText())) {
                return (JButton) child;
            }
            if (child instanceof Container) {
                final JButton found = findButton((Container) child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
