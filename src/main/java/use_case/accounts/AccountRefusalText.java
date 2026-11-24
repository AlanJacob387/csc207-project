package use_case.accounts;

import entity.AccountRules;

/**
 * The wording for an {@link AccountRules.Refusal}.
 *
 * The rule lives in the entity and reports a reason; the sentence a user reads is
 * written here, once, for the three use cases that check an account: sign up, the
 * administrator's Create User and Change Password. That keeps the entity free of
 * presentation text and keeps the three pages from drifting into different
 * phrasings of the same rule.
 *
 * The numbers in the sentences are read from {@link AccountRules}, so tightening
 * the rule changes the wording with it.
 */
public final class AccountRefusalText {

    /** Utility class: not instantiable. */
    private AccountRefusalText() {
    }

    /**
     * Words a refusal.
     *
     * The switch is an expression with no {@code default}, so a reason added to
     * {@link AccountRules.Refusal} stops this class compiling until it has a
     * sentence - a new refusal cannot reach a page worded as if it were a password
     * problem.
     *
     * @param refusal the reason, possibly null
     * @return the sentence to show, or an empty string when there is no refusal
     */
    public static String forRefusal(AccountRules.Refusal refusal) {
        if (refusal == null) {
            return "";
        }
        return switch (refusal) {
            case USERNAME_MISSING -> "Choose a username.";
            case USERNAME_LENGTH -> "Usernames are " + AccountRules.USERNAME_MIN + " to "
                    + AccountRules.USERNAME_MAX + " characters.";
            case USERNAME_CHARACTERS ->
                    "Use only letters, digits, dots, underscores and hyphens in a username.";
            case DISPLAY_NAME_LENGTH -> "Display names are at most "
                    + AccountRules.DISPLAY_NAME_MAX + " characters.";
            case PASSWORD_MISSING -> "Choose a password.";
            case PASSWORD_LENGTH -> "Passwords are " + AccountRules.PASSWORD_MIN + " to "
                    + AccountRules.PASSWORD_MAX + " characters.";
        };
    }
}
