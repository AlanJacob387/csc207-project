package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;

import use_case.database_settings.DatabaseSettingsDataAccessInterface;

/**
 * The storage half of the Database Settings use case: it answers how many
 * accounts a pair of settings can see, writes the settings for this user, and
 * knows which URL is configured and where the file lives.
 *
 * It reports facts and no sentences, so the wording of a problem stays in the use
 * case. The actual connection and file work is done by
 * {@link SupabaseAppDataAccessObject} and {@link DatabaseConfig}.
 */
public class DatabaseSettingsGateway implements DatabaseSettingsDataAccessInterface {

    @Override
    public int countAccounts(String url, String key) {
        final DatabaseConfig config = validated(url, key);
        return new SupabaseAppDataAccessObject(config.getUrl(), config.getKey())
                .getAllUsers().size();
    }

    @Override
    public void save(String url, String key) {
        final DatabaseConfig config = validated(url, key);
        try {
            DatabaseConfig.save(config.getUrl(), config.getKey());
        }
        catch (IOException ex) {
            throw new UncheckedIOException("the settings file could not be written ("
                    + ex.getMessage() + ")", ex);
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
        final DatabaseConfig config = DatabaseConfig.resolve();
        return config == null ? "" : config.getUrl();
    }

    @Override
    public String settingsFile() {
        return DatabaseConfig.userFile().toString();
    }

    /**
     * Applies the same rules the environment and file paths apply before anything
     * is sent, and reports a refusal as the failure this port promises rather than
     * as the argument error {@link DatabaseConfig} raises for its own callers.
     *
     * The use case asks the same questions before calling in, so this is the
     * backstop rather than the way a person normally meets the rule.
     *
     * @param url the project URL
     * @param key the publishable key
     * @return the validated settings
     * @throws IllegalStateException when the values are missing or refused
     */
    private static DatabaseConfig validated(String url, String key) {
        try {
            final DatabaseConfig config = DatabaseConfig.fromValues(url, key, "the settings page");
            if (config == null) {
                throw new IllegalStateException("the settings were incomplete");
            }
            return config;
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }
}
