package use_case.database_settings;

/**
 * What the settings use case needs from storage.
 *
 * It reports facts - how many accounts the settings can see, whether a save
 * worked, which URL is configured, where the file goes - and no sentences, so the
 * wording of a failure stays in the use case.
 */
public interface DatabaseSettingsDataAccessInterface {

    /**
     * Reads the stored accounts with the given settings, to see whether they work.
     *
     * @param url the project URL
     * @param key the publishable key
     * @return how many accounts the settings can see
     * @throws IllegalStateException when the database rejects the settings
     * @throws java.io.UncheckedIOException when the database cannot be reached
     */
    int countAccounts(String url, String key);

    /**
     * Saves the settings for this user.
     *
     * @param url the project URL
     * @param key the publishable key
     * @throws IllegalStateException when they cannot be written
     */
    void save(String url, String key);

    /**
     * The URL that is already configured.
     *
     * @return the URL, or an empty string when nothing is configured
     * @throws IllegalStateException when settings are configured but cannot be used:
     *                               the file cannot be read, or it holds a key or URL
     *                               the rules refuse
     */
    String currentUrl();

    /**
     * Where saving writes, so the page can say where the settings went.
     *
     * @return the settings file path
     */
    String settingsFile();

    /**
     * Says whether a key would bypass row level security.
     *
     * A fact, not a rule the use case invents: the page asks, and the answer is the
     * same decision that guards the environment variables and the settings file, so
     * the two ways in cannot drift apart.
     *
     * @param key the pasted key
     * @return true for a secret key, in either format Supabase issues
     */
    boolean isSecretKey(String key);

    /**
     * Says whether a URL can carry the key without exposing it.
     *
     * @param url the pasted project URL
     * @return true for HTTPS, and for plain HTTP to this machine
     */
    boolean isSecureUrl(String url);
}
