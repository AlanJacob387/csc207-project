package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests the one account rule: what a username, a display name and a password may
 * be, and that both ways of writing a credential refuse the same values.
 */
class AccountRulesTest {

    @Test
    void anOrdinaryAccountIsAccepted() {
        assertNull(AccountRules.refusalFor("coach1", "Coach One", "teampass"));
    }

    @Test
    void aBlankDisplayNameIsAllowedBecauseThePageFallsBackToTheUsername() {
        assertNull(AccountRules.displayNameRefusalFor("   "));
        assertNull(AccountRules.displayNameRefusalFor(null));
    }

    @Test
    void aUsernameHasALengthAndACharacterSet() {
        assertEquals("Choose a username.", AccountRules.usernameRefusalFor(""));
        assertEquals("Choose a username.", AccountRules.usernameRefusalFor("   "));
        assertEquals("Usernames are 3 to 40 characters.", AccountRules.usernameRefusalFor("ab"));
        assertEquals("Usernames are 3 to 40 characters.",
                AccountRules.usernameRefusalFor("x".repeat(41)));
        assertEquals("Use only letters, digits, dots, underscores and hyphens in a username.",
                AccountRules.usernameRefusalFor("pat player"));
        assertEquals("Use only letters, digits, dots, underscores and hyphens in a username.",
                AccountRules.usernameRefusalFor("pat@home"));

        assertNull(AccountRules.usernameRefusalFor("abc"));
        assertNull(AccountRules.usernameRefusalFor("x".repeat(40)));
        assertNull(AccountRules.usernameRefusalFor("pat.smith_1-2"));
        assertNull(AccountRules.usernameRefusalFor("  coach1  "));
    }

    @Test
    void aDisplayNameHasAMaximum() {
        assertEquals("Display names are at most 60 characters.",
                AccountRules.displayNameRefusalFor("n".repeat(61)));

        assertNull(AccountRules.displayNameRefusalFor("n".repeat(60)));
    }

    @Test
    void aPasswordHasAMinimumAndAMaximum() {
        assertEquals("Choose a password.", AccountRules.passwordRefusalFor(""));
        assertEquals("Passwords are 8 to 128 characters.", AccountRules.passwordRefusalFor("pw"));
        assertEquals("Passwords are 8 to 128 characters.",
                AccountRules.passwordRefusalFor("p".repeat(129)));

        assertNull(AccountRules.passwordRefusalFor("teampass"));
        assertNull(AccountRules.passwordRefusalFor("p".repeat(8)));
        assertNull(AccountRules.passwordRefusalFor("p".repeat(128)));
    }

    @Test
    void theFirstRefusalIsReportedSoTheUserFixesOneThingAtATime() {
        assertEquals("Usernames are 3 to 40 characters.",
                AccountRules.refusalFor("ab", "n".repeat(61), "pw"));
        assertEquals("Display names are at most 60 characters.",
                AccountRules.refusalFor("coach1", "n".repeat(61), "pw"));
    }

    @Test
    void creatingAnAccountRefusesARuleThePageWouldAlsoRefuse() {
        final UserFactory factory = new UserFactory();

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> factory.create("coach1", "pw", "Coach One", Role.COACH));

        assertTrue(thrown.getMessage().startsWith("Passwords are 8 to 128"), thrown.getMessage());
    }

    @Test
    void changingAPasswordRefusesTheSameRule() {
        final User user = new User("coach1", "pbkdf2$1000$c2FsdA==$aGFzaA==", "Coach One",
                Role.COACH);

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> user.withNewPassword("short"));

        assertTrue(thrown.getMessage().startsWith("Passwords are 8 to 128"), thrown.getMessage());
    }
}
