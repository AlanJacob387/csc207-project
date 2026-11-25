package use_case.accounts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import entity.AccountRules;

/**
 * Tests the words the three account pages show for a refused rule. The numbers in
 * each sentence come from {@link AccountRules}, so this also pins that the wording
 * follows the rule.
 */
class AccountRefusalTextTest {

    @Test
    void everyRefusalHasASentence() {
        for (AccountRules.Refusal refusal : AccountRules.Refusal.values()) {
            final String sentence = AccountRefusalText.forRefusal(refusal);

            assertTrue(sentence.length() > 0, "no sentence for " + refusal);
            assertTrue(sentence.endsWith("."), "a sentence ends in a full stop: " + sentence);
        }
    }

    @Test
    void theSentencesSayWhatToFix() {
        assertEquals("Choose a username.",
                AccountRefusalText.forRefusal(AccountRules.Refusal.USERNAME_MISSING));
        assertEquals("Usernames are " + AccountRules.USERNAME_MIN + " to "
                        + AccountRules.USERNAME_MAX + " characters.",
                AccountRefusalText.forRefusal(AccountRules.Refusal.USERNAME_LENGTH));
        assertEquals("Use only letters, digits, dots, underscores and hyphens in a username.",
                AccountRefusalText.forRefusal(AccountRules.Refusal.USERNAME_CHARACTERS));
        assertEquals("Display names are at most " + AccountRules.DISPLAY_NAME_MAX
                        + " characters.",
                AccountRefusalText.forRefusal(AccountRules.Refusal.DISPLAY_NAME_LENGTH));
        assertEquals("Choose a password.",
                AccountRefusalText.forRefusal(AccountRules.Refusal.PASSWORD_MISSING));
        assertEquals("Passwords are " + AccountRules.PASSWORD_MIN + " to "
                        + AccountRules.PASSWORD_MAX + " characters.",
                AccountRefusalText.forRefusal(AccountRules.Refusal.PASSWORD_LENGTH));
    }

    @Test
    void noRefusalShowsNothing() {
        assertEquals("", AccountRefusalText.forRefusal(null));
    }
}
