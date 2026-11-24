package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests the one account rule: what a username, a display name and a password may
 * be, that it reports a reason rather than a sentence, and that both ways of
 * writing a credential refuse the same values.
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
        assertEquals(AccountRules.Refusal.USERNAME_MISSING,
                AccountRules.usernameRefusalFor(""));
        assertEquals(AccountRules.Refusal.USERNAME_MISSING,
                AccountRules.usernameRefusalFor("   "));
        assertEquals(AccountRules.Refusal.USERNAME_LENGTH,
                AccountRules.usernameRefusalFor("ab"));
        assertEquals(AccountRules.Refusal.USERNAME_LENGTH,
                AccountRules.usernameRefusalFor("x".repeat(41)));
        assertEquals(AccountRules.Refusal.USERNAME_CHARACTERS,
                AccountRules.usernameRefusalFor("pat player"));
        assertEquals(AccountRules.Refusal.USERNAME_CHARACTERS,
                AccountRules.usernameRefusalFor("pat@home"));

        assertNull(AccountRules.usernameRefusalFor("abc"));
        assertNull(AccountRules.usernameRefusalFor("x".repeat(40)));
        assertNull(AccountRules.usernameRefusalFor("pat.smith_1-2"));
        assertNull(AccountRules.usernameRefusalFor("  coach1  "));
    }

    @Test
    void aDisplayNameHasAMaximum() {
        assertEquals(AccountRules.Refusal.DISPLAY_NAME_LENGTH,
                AccountRules.displayNameRefusalFor("n".repeat(61)));

        assertNull(AccountRules.displayNameRefusalFor("n".repeat(60)));
    }

    @Test
    void aPasswordHasAMinimumAndAMaximum() {
        assertEquals(AccountRules.Refusal.PASSWORD_MISSING,
                AccountRules.passwordRefusalFor(""));
        assertEquals(AccountRules.Refusal.PASSWORD_LENGTH,
                AccountRules.passwordRefusalFor("pw"));
        assertEquals(AccountRules.Refusal.PASSWORD_LENGTH,
                AccountRules.passwordRefusalFor("p".repeat(129)));

        assertNull(AccountRules.passwordRefusalFor("teampass"));
        assertNull(AccountRules.passwordRefusalFor("p".repeat(8)));
        assertNull(AccountRules.passwordRefusalFor("p".repeat(128)));
    }

    @Test
    void theFirstRefusalIsReportedSoTheUserFixesOneThingAtATime() {
        assertEquals(AccountRules.Refusal.USERNAME_LENGTH,
                AccountRules.refusalFor("ab", "n".repeat(61), "pw"));
        assertEquals(AccountRules.Refusal.DISPLAY_NAME_LENGTH,
                AccountRules.refusalFor("coach1", "n".repeat(61), "pw"));
    }

    @Test
    void creatingAnAccountRefusesARuleThePageWouldAlsoRefuse() {
        final UserFactory factory = new UserFactory();

        final InvalidAccountException thrown = assertThrows(InvalidAccountException.class,
                () -> factory.create("coach1", "pw", "Coach One", Role.COACH));

        assertEquals(AccountRules.Refusal.PASSWORD_LENGTH, thrown.getRefusal());
    }

    @Test
    void changingAPasswordRefusesTheSameRule() {
        final User user = new User("coach1", "pbkdf2$1000$c2FsdA==$aGFzaA==", "Coach One",
                Role.COACH);

        final InvalidAccountException thrown = assertThrows(InvalidAccountException.class,
                () -> user.withNewPassword("short"));

        assertEquals(AccountRules.Refusal.PASSWORD_LENGTH, thrown.getRefusal());
    }
}
