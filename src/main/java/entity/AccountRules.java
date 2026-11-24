package entity;

/**
 * The one rule for what an acceptable account looks like.
 *
 * Sign Up, the administrator's Create User and Change Password all ask this
 * class, so a name or a password accepted on one page is accepted on the others.
 *
 * It reports a {@link Refusal} rather than a sentence: the entity holds no
 * wording, and the use-case layer turns a refusal into the message it shows
 * (see {@code use_case.accounts.AccountRefusalText}).
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

    /** Why a set of account details is not acceptable. */
    public enum Refusal {

        /** No username was given. */
        USERNAME_MISSING,

        /** The username is shorter than the minimum or longer than the maximum. */
        USERNAME_LENGTH,

        /** The username contains a character the rule does not allow. */
        USERNAME_CHARACTERS,

        /** The display name is longer than the maximum. */
        DISPLAY_NAME_LENGTH,

        /** No password was given. */
        PASSWORD_MISSING,

        /** The password is shorter than the minimum or longer than the maximum. */
        PASSWORD_LENGTH
    }

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
    public static Refusal refusalFor(String username, String displayName, String password) {
        final Refusal nameRefusal = usernameRefusalFor(username);
        if (nameRefusal != null) {
            return nameRefusal;
        }
        final Refusal displayRefusal = displayNameRefusalFor(displayName);
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
    public static Refusal usernameRefusalFor(String username) {
        final String name = username == null ? "" : username.trim();
        if (name.isEmpty()) {
            return Refusal.USERNAME_MISSING;
        }
        if (name.length() < USERNAME_MIN || name.length() > USERNAME_MAX) {
            return Refusal.USERNAME_LENGTH;
        }
        if (!name.matches(USERNAME_CHARACTERS)) {
            return Refusal.USERNAME_CHARACTERS;
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
    public static Refusal displayNameRefusalFor(String displayName) {
        final String name = displayName == null ? "" : displayName.trim();
        if (name.length() > DISPLAY_NAME_MAX) {
            return Refusal.DISPLAY_NAME_LENGTH;
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
    public static Refusal passwordRefusalFor(String password) {
        final String value = password == null ? "" : password;
        if (value.isEmpty()) {
            return Refusal.PASSWORD_MISSING;
        }
        if (value.length() < PASSWORD_MIN || value.length() > PASSWORD_MAX) {
            return Refusal.PASSWORD_LENGTH;
        }
        return null;
    }
}
