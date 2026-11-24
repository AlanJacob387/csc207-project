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
        return new SupabaseAppDataAccessObject(url, key).getAllUsers().size();
    }

    @Override
    public void save(String url, String key) {
        try {
            DatabaseConfig.save(url.trim(), key.trim());
        }
        catch (IOException ex) {
            throw new UncheckedIOException("the settings file could not be written ("
                    + ex.getMessage() + ")", ex);
        }
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
}
