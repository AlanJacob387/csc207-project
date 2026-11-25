package data_access;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.UUID;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live proof that the app can reach the hosted database.
 *
 * Tagged "network", so a normal {@code gradlew test} stays offline. Run it with
 * {@code gradlew test -Pnetwork} after setting {@code TEAMSPORTS_DB_URL} and
 * {@code TEAMSPORTS_DB_KEY}; when they are absent the test is skipped rather than
 * failed.
 *
 * It only reads. Every table policy grants select, insert and update but no
 * delete, so a test that wrote rows could not clean up after itself.
 */
@Tag("network")
class SupabaseAppDataAccessObjectLiveTest {

    @Test
    void readsFromTheHostedDatabase() {
        final SupabaseAppDataAccessObject store = SupabaseAppDataAccessObject.fromConfig();
        assumeTrue(store != null, "set " + SupabaseAppDataAccessObject.URL_VARIABLE
                + " and " + SupabaseAppDataAccessObject.KEY_VARIABLE + " to run this test");

        final String missing = "no-such-user-" + UUID.randomUUID();

        assertNull(store.get(missing), "an unknown username must read as null");
        assertFalse(store.existsByName(missing));
        System.out.println("Supabase answered: " + store.getAllUsers().size()
                + " account(s) in the users table");
    }
}
