package entity;

/**
 * The one rule for what an acceptable account looks like.
 *
 * Sign Up, the administrator's Create User and Change Password all ask this
 * class, so a name or a password accepted on one page is accepted on the others
 * and the sentence explaining a refusal is written once.
 *
 * The rule is applied when an account is created and when a password is
 * changed, never at login. An account written before a rule existed therefore
 * still signs in, which is what keeps an older store usable after a rule gets
 * stricter: only new credentials have to satisfy it.
 */
public final class AccountRules {

    /** Shortest accepted username. */
    public static final int USERNAME_MIN = 3;

    /** Longest accepted username, so a row and a URL stay readable. */
    public static final int USERNAME_MAX = 40;

    /** Longest accepted display name. */
    public static final int DISPLAY_NAME_MAX = 60;

    /** Shortest accepted password. */
    public static final int PASSWORD_MIN = 8;

    /** Longest accepted password, to bound what the hash derives from. */
    public static final int PASSWORD_MAX = 128;

    /** Characters a username may contain: letters, digits, dot, underscore, hyphen. */
    private static final String USERNAME_CHARACTERS = "^[A-Za-z0-9._-]+$";

    /** Utility class: not instantiable. */
    private AccountRules() {
    }

    /**
     * Checks a whole new account.
     *
     * @param username    the chosen username, already trimmed
     * @param displayName the chosen display name, already trimmed
     * @param password    the chosen password
     * @return the first refusal, or null when the account is acceptable
     */
    public static String refusalFor(String username, String displayName, String password) {
        final String nameRefusal = usernameRefusalFor(username);
        if (nameRefusal != null) {
            return nameRefusal;
        }
        final String displayRefusal = displayNameRefusalFor(displayName);
        if (displayRefusal != null) {
            return displayRefusal;
        }
        return passwordRefusalFor(password);
    }

    /**
     * Checks a username.
     *
     * @param username the chosen username
     * @return the refusal, or null when it is acceptable
     */
    public static String usernameRefusalFor(String username) {
        final String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            return "Choose a username.";
        }
        if (name.length() < USERNAME_MIN || name.length() > USERNAME_MAX) {
            return "Usernames are " + USERNAME_MIN + " to " + USERNAME_MAX + " characters.";
        }
        if (!name.matches(USERNAME_CHARACTERS)) {
            return "Use only letters, digits, dots, underscores and hyphens in a username.";
        }
        return null;
    }

    /**
     * Checks a display name. A blank one is allowed, because the calling page
     * falls back to the username.
     *
     * @param displayName the chosen display name
     * @return the refusal, or null when it is acceptable
     */
    public static String displayNameRefusalFor(String displayName) {
        final String name = displayName == null ? "" : displayName.trim();
        if (name.length() > DISPLAY_NAME_MAX) {
            return "Display names are at most " + DISPLAY_NAME_MAX + " characters.";
        }
        return null;
    }

    /**
     * Checks a password. Length is the whole rule: there is no requirement for
     * a digit or a symbol, which the documentation says plainly rather than
     * implying a strength this app cannot measure.
     *
     * @param password the chosen password
     * @return the refusal, or null when it is acceptable
     */
    public static String passwordRefusalFor(String password) {
        final String value = password == null ? "" : password;
        if (value.isEmpty()) {
            return "Choose a password.";
        }
        if (value.length() < PASSWORD_MIN || value.length() > PASSWORD_MAX) {
            return "Passwords are " + PASSWORD_MIN + " to " + PASSWORD_MAX + " characters.";
        }
        return null;
    }
}
