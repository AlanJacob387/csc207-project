package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests where the database settings are found: the environment first, then the
 * system properties, then the file the settings page writes. Also tests that a
 * key which bypasses row level security - in either of Supabase's two formats -
 * or a URL that is not HTTPS is refused before it can be saved.
 */
class DatabaseConfigTest {

    @TempDir
    Path tempDir;

    private static Map<String, String> env(String url, String key) {
        final Map<String, String> environment = new HashMap<>();
        if (url != null) {
            environment.put(DatabaseConfig.URL_VARIABLE, url);
        }
        if (key != null) {
            environment.put(DatabaseConfig.KEY_VARIABLE, key);
        }
        return environment;
    }

    private static Properties properties(String url, String key) {
        final Properties properties = new Properties();
        if (url != null) {
            properties.setProperty(DatabaseConfig.URL_PROPERTY, url);
        }
        if (key != null) {
            properties.setProperty(DatabaseConfig.KEY_PROPERTY, key);
        }
        return properties;
    }

    @Test
    void theEnvironmentWinsOverEverythingElse() throws IOException {
        final Path file = DatabaseConfig.save(tempDir.resolve("database.properties"),
                "https://file.example", "file-key");

        final DatabaseConfig config = DatabaseConfig.resolve(
                env("https://env.example", "env-key"),
                properties("https://prop.example", "prop-key"), file);

        assertEquals("https://env.example", config.getUrl());
        assertEquals("env-key", config.getKey());
        assertEquals("environment", config.getSource());
    }

    @Test
    void theSystemPropertiesComeBeforeTheFile() throws IOException {
        final Path file = DatabaseConfig.save(tempDir.resolve("database.properties"),
                "https://file.example", "file-key");

        final DatabaseConfig config = DatabaseConfig.resolve(env(null, null),
                properties("https://prop.example", "prop-key"), file);

        assertEquals("https://prop.example", config.getUrl());
        assertEquals("system properties", config.getSource());
    }

    @Test
    void theSavedFileIsUsedWhenNothingElseIsSet() throws IOException {
        final Path file = DatabaseConfig.save(tempDir.resolve("database.properties"),
                "https://file.example", "file-key");

        final DatabaseConfig config = DatabaseConfig.resolve(env(null, null),
                properties(null, null), file);

        assertEquals("https://file.example", config.getUrl());
        assertEquals(file.toString(), config.getSource());
    }

    @Test
    void nothingConfiguredResolvesToNull() {
        assertNull(DatabaseConfig.resolve(env(null, null), properties(null, null),
                tempDir.resolve("missing.properties")));
    }

    @Test
    void anIncompletePairIsIgnored() {
        assertNull(DatabaseConfig.fromValues("https://x.example", "   ", "test"));
        assertNull(DatabaseConfig.fromValues(null, "key", "test"));
    }

    @Test
    void aSecretKeyIsRefused() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.fromValues("https://x.example", "sb_secret_abc123", "test"));

        assertTrue(thrown.getMessage().contains("publishable"));
        assertTrue(thrown.getMessage().contains("row level security"));
    }

    @Test
    void aLegacyServiceRoleKeyIsRefusedToo() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.fromValues("https://x.example",
                        legacyKey("service_role"), "test"));

        assertTrue(thrown.getMessage().contains("row level security"),
                "the old service_role JWT is as powerful as the new secret key");
    }

    @Test
    void aLegacyAnonKeyIsAccepted() {
        final String key = legacyKey("anon");

        final DatabaseConfig config = DatabaseConfig.fromValues("https://x.example", key, "test");

        assertEquals(key, config.getKey(), "the legacy anon key is the low-privilege key");
    }

    @Test
    void aKeyThatIsNotAJwtIsLeftForTheDatabaseToJudge() {
        final DatabaseConfig config =
                DatabaseConfig.fromValues("https://x.example", "not-a-key", "test");

        assertEquals("not-a-key", config.getKey());
    }

    @Test
    void aUrlWithoutASchemeIsRefused() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.fromValues("x.supabase.co", "sb_publishable_key", "test"));

        assertTrue(thrown.getMessage().contains("https://"));
    }

    @Test
    void aPlainHttpUrlIsRefusedBecauseTheKeyWouldTravelInTheClear() {
        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> DatabaseConfig.fromValues("http://x.example", "sb_publishable_key", "test"));

        assertTrue(thrown.getMessage().contains("https://"));
    }

    @Test
    void settingsRoundTripThroughTheSavedFile() throws IOException {
        final Path file = tempDir.resolve("nested").resolve("database.properties");

        DatabaseConfig.save(file, "  https://round.example  ", " sb_publishable_key ");

        final DatabaseConfig loaded = DatabaseConfig.load(file);
        assertEquals("https://round.example", loaded.getUrl());
        assertEquals("sb_publishable_key", loaded.getKey());
        assertTrue(Files.exists(file));
    }

    @Test
    void aFileWithoutBothValuesIsRejected() throws IOException {
        final Path file = tempDir.resolve("half.properties");
        Files.writeString(file, "url=https://x.example\n");

        assertThrows(IOException.class, () -> DatabaseConfig.load(file));
    }

    @Test
    void theSettingsFileSitsInTheHomeFolder() {
        final Path file = DatabaseConfig.userFile();

        assertEquals(DatabaseConfig.FILE_NAME, file.getFileName().toString());
        assertTrue(file.toString().contains(DatabaseConfig.FOLDER));
    }

    /**
     * Builds a legacy Supabase key: a JWT with the given role in its payload. The
     * signature is not checked by anything here, so a placeholder is enough.
     *
     * @param role the role the key would run as
     * @return a three-part base64url key, the shape the dashboard shows
     */
    private static String legacyKey(String role) {
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final String header = encoder.encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        final String payload = encoder.encodeToString(
                ("{\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".c2lnbmF0dXJl";
    }
}
