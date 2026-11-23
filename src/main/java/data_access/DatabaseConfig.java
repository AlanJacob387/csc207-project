package data_access;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Where the app finds its database connection, and where it stores one.
 *
 * Looked up in this order; the first complete pair wins:
 *
 * <ol>
 *   <li>{@code TEAMSPORTS_DB_URL} and {@code TEAMSPORTS_DB_KEY} in the
 *       environment - for CI and for overriding everything else</li>
 *   <li>{@code -Dteamsports.db.url} and {@code -Dteamsports.db.key}</li>
 *   <li>{@code %USERPROFILE%\.team-sports\database.properties}, written by the
 *       in-app settings page so nobody has to open a terminal</li>
 * </ol>
 *
 * Nothing is read from the repository: the file lives in the user's home folder,
 * so the publishable key never reaches git. When nothing is configured,
 * {@link #resolve()} returns null and AppBuilder keeps the local JSON file.
 */
public final class DatabaseConfig {

    /** Environment variable holding the project URL. */
    public static final String URL_VARIABLE = "TEAMSPORTS_DB_URL";

    /** Environment variable holding the publishable key. */
    public static final String KEY_VARIABLE = "TEAMSPORTS_DB_KEY";

    /** System property holding the project URL. */
    public static final String URL_PROPERTY = "teamsports.db.url";

    /** System property holding the publishable key. */
    public static final String KEY_PROPERTY = "teamsports.db.key";

    /** Folder, inside the user's home, that holds the saved settings. */
    public static final String FOLDER = ".team-sports";

    /** Name of the saved settings file. */
    public static final String FILE_NAME = "database.properties";

    /** Prefix of a key a desktop application must never use. */
    private static final String SECRET_PREFIX = "sb_secret_";

    private static final String URL_ENTRY = "url";
    private static final String KEY_ENTRY = "key";

    private final String url;
    private final String key;
    private final String source;

    private DatabaseConfig(String url, String key, String source) {
        this.url = url;
        this.key = key;
        this.source = source;
    }

    /**
     * Finds the connection settings from the environment, the system properties
     * and the saved file, in that order.
     *
     * @return the settings, or null when nothing is configured
     * @throws IllegalStateException if a settings file exists but cannot be read
     */
    public static DatabaseConfig resolve() {
        return resolve(System.getenv(), System.getProperties(), userFile());
    }

    /**
     * Finds the connection settings from explicit sources, so the lookup order
     * can be tested without touching the real environment.
     *
     * @param environment the environment variables
     * @param properties  the system properties
     * @param userFile    the saved settings file, or null to skip it
     * @return the settings, or null when nothing is configured
     */
    static DatabaseConfig resolve(Map<String, String> environment, Properties properties,
                                  Path userFile) {
        DatabaseConfig config = fromValues(environment.get(URL_VARIABLE),
                environment.get(KEY_VARIABLE), "environment");
        if (config != null) {
            return config;
        }
        config = fromValues(properties.getProperty(URL_PROPERTY),
                properties.getProperty(KEY_PROPERTY), "system properties");
        if (config != null) {
            return config;
        }
        if (userFile != null && Files.isReadable(userFile)) {
            try {
                return load(userFile);
            }
            catch (IOException ex) {
                throw new IllegalStateException("Could not read " + userFile + ": "
                        + ex.getMessage(), ex);
            }
        }
        return null;
    }

    /**
     * Builds settings from two values.
     *
     * @param url    the project URL, possibly blank
     * @param key    the publishable key, possibly blank
     * @param source where the values came from, for the status label
     * @return the settings, or null when either value is blank
     */
    public static DatabaseConfig fromValues(String url, String key, String source) {
        if (url == null || url.isBlank() || key == null || key.isBlank()) {
            return null;
        }
        return new DatabaseConfig(validateUrl(url.trim()), validateKey(key.trim()), source);
    }

    /**
     * Reads settings from a file, so a teammate can send one over chat instead of
     * dictating a key.
     *
     * @param file the properties file
     * @return the settings
     * @throws IOException if the file cannot be read or lacks a value
     */
    public static DatabaseConfig load(Path file) throws IOException {
        final Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            properties.load(in);
        }
        final DatabaseConfig config = fromValues(properties.getProperty(URL_ENTRY),
                properties.getProperty(KEY_ENTRY), file.toString());
        if (config == null) {
            throw new IOException("Both " + URL_ENTRY + " and " + KEY_ENTRY
                    + " are needed in " + file);
        }
        return config;
    }

    /**
     * Saves settings for this user, so the next start connects without any
     * terminal work.
     *
     * @param url the project URL
     * @param key the publishable key
     * @return the file that was written
     * @throws IOException if the file cannot be written
     */
    public static Path save(String url, String key) throws IOException {
        return save(userFile(), url, key);
    }

    /**
     * Saves settings to a specific file.
     *
     * @param file the file to write
     * @param url  the project URL
     * @param key  the publishable key
     * @return the file that was written
     * @throws IOException if the file cannot be written
     */
    static Path save(Path file, String url, String key) throws IOException {
        final DatabaseConfig config = fromValues(url, key, file.toString());
        if (config == null) {
            throw new IllegalArgumentException("Enter both a URL and a publishable key.");
        }
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        final Properties properties = new Properties();
        properties.setProperty(URL_ENTRY, config.getUrl());
        properties.setProperty(KEY_ENTRY, config.getKey());
        try (OutputStream out = Files.newOutputStream(file)) {
            properties.store(out, "Team Sports database connection");
        }
        return file;
    }

    /**
     * The per-user settings file.
     *
     * @return the file inside the user's home folder
     */
    public static Path userFile() {
        return Path.of(System.getProperty("user.home"), FOLDER, FILE_NAME);
    }

    /**
     * Refuses a secret key, which bypasses row level security and must never be
     * shipped in a desktop application.
     *
     * @param key the pasted key
     * @return the same key when it is a publishable key
     */
    public static String validateKey(String key) {
        if (key.startsWith(SECRET_PREFIX)) {
            throw new IllegalArgumentException("That is a secret key. A secret key bypasses row "
                    + "level security and must not be used in a desktop app; paste the "
                    + "publishable key (sb_publishable_...) instead.");
        }
        return key;
    }

    /**
     * Refuses a URL that is not an HTTP address, so a typo fails with a clear
     * message instead of an unreadable one at the first request.
     *
     * @param url the pasted project URL
     * @return the same URL when it looks usable
     */
    public static String validateUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            throw new IllegalArgumentException("The project URL must start with https://");
        }
        return url;
    }

    public String getUrl() {
        return url;
    }

    public String getKey() {
        return key;
    }

    /**
     * Says where the settings came from.
     *
     * @return "environment", "system properties", or the settings file path
     */
    public String getSource() {
        return source;
    }
}
