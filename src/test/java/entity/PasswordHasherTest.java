package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests password hashing: a stored credential never contains the plaintext, the
 * same password hashes differently every time, verification works, and a legacy
 * plaintext credential still verifies so an existing store keeps working.
 */
class PasswordHasherTest {

    @Test
    void aHashHidesThePasswordAndStillVerifies() {
        final String stored = PasswordHasher.hash("pw");

        assertNotEquals("pw", stored);
        assertTrue(stored.startsWith("pbkdf2$"));
        assertTrue(PasswordHasher.isHashed(stored));
        assertTrue(PasswordHasher.matches("pw", stored));
        assertFalse(PasswordHasher.matches("nope", stored));
    }

    @Test
    void theSamePasswordHashesDifferentlyEveryTime() {
        assertNotEquals(PasswordHasher.hash("pw"), PasswordHasher.hash("pw"));
    }

    @Test
    void aLegacyPlaintextCredentialStillVerifies() {
        assertTrue(PasswordHasher.matches("pw", "pw"));
        assertFalse(PasswordHasher.matches("nope", "pw"));
        assertFalse(PasswordHasher.isHashed("pw"));
    }

    @Test
    void missingAndMalformedCredentialsNeverMatch() {
        assertFalse(PasswordHasher.matches(null, "pw"));
        assertFalse(PasswordHasher.matches("pw", null));
        assertFalse(PasswordHasher.matches("pw", ""));
        assertFalse(PasswordHasher.matches("pw", "pbkdf2$notanumber$c2FsdA==$aGFzaA=="));
        assertFalse(PasswordHasher.matches("pw", "pbkdf2$1000$!!!$!!!"));
        assertFalse(PasswordHasher.matches("pw", "pbkdf2$1000"));
    }

    @Test
    void hashingRequiresAPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(null));
    }

    @Test
    void changingAPasswordRehashesItAndKeepsTheOtherFields() {
        final User user = new User("p1", "legacy", "Pat", Role.PLAYER).withActive(false);

        final User updated = user.withNewPassword("secret");

        assertTrue(updated.matches("secret"));
        assertFalse(updated.matches("legacy"));
        assertTrue(PasswordHasher.isHashed(updated.getPassword()));
        assertEquals("p1", updated.getUsername());
        assertEquals("Pat", updated.getDisplayName());
        assertEquals(Role.PLAYER, updated.getRole());
        assertFalse(updated.isActive());
    }

    @Test
    void changingTheRoleOrStatusKeepsTheCredential() {
        final User user = new User("p1", "legacy", "Pat", Role.PLAYER);

        final User coach = user.withRole(Role.COACH).withActive(false);

        assertEquals("legacy", coach.getPassword());
        assertEquals(Role.COACH, coach.getRole());
        assertFalse(coach.isActive());
        assertTrue(coach.matches("legacy"));
    }
}
