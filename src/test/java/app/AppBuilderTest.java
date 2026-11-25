package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.User;
import entity.UserFactory;

/**
 * Tests the demo seeding that runs while the app is starting: it fills a fresh
 * store, never overwrites an account that is already there, and turns a store it
 * cannot reach into a message instead of an exception, so the sign-in page can
 * open and explain itself.
 */
class AppBuilderTest {

    @TempDir
    Path tempDir;

    private FileAppDataAccessObject store() {
        return new FileAppDataAccessObject(tempDir.resolve("app-data.json"));
    }

    @Test
    void aFreshStoreGetsTheFourDemoAccountsAndTheDemoTeam() {
        final FileAppDataAccessObject store = store();

        assertEquals("", AppBuilder.seedDemoDataIfEmpty(store, new UserFactory()));

        assertEquals(4, store.getAllUsers().size());
        assertTrue(store.get("coach1").matches("teampass"), "the demo password is hashed and still works");
        assertEquals(Role.ADMIN, store.get("admin1").getRole());
        assertNotNull(store.getTeam("demo-team"));
        assertEquals(1, store.getTeam("demo-team").getPlayerUsernames().size());
    }

    @Test
    void anAccountThatAlreadyExistsIsNeverOverwritten() {
        final FileAppDataAccessObject store = store();
        AppBuilder.seedDemoDataIfEmpty(store, new UserFactory());
        store.save(new User("player1", store.get("player1").getPassword(), "Pat Custom",
                Role.PLAYER));

        assertEquals("", AppBuilder.seedDemoDataIfEmpty(store, new UserFactory()));

        assertEquals("Pat Custom", store.get("player1").getDisplayName(),
                "a customised account survives the next start");
        assertEquals(4, store.getAllUsers().size());
    }

    @Test
    void aDeletedDemoAccountStaysDeletedOnceTheStoreHoldsARealAccount() {
        final FileAppDataAccessObject store = store();
        store.createUser(new UserFactory().create("real.coach", "a-real-password", "Real Coach",
                Role.COACH));

        assertEquals("", AppBuilder.seedDemoDataIfEmpty(store, new UserFactory()));

        assertEquals(1, store.getAllUsers().size(),
                "admin1 has a published password, so it must not be re-created beside real data");
        assertNull(store.get("admin1"));
        assertNull(store.getTeam("demo-team"));
    }

    @Test
    void aStoreThatHoldsOnlyDemoAccountsIsCompletedAndKeepsWhatIsThere() {
        final FileAppDataAccessObject store = store();
        store.createUser(new UserFactory().create("coach1", "kept-password", "Coach Kept",
                Role.COACH));

        assertEquals("", AppBuilder.seedDemoDataIfEmpty(store, new UserFactory()));

        assertEquals(4, store.getAllUsers().size());
        assertEquals("Coach Kept", store.get("coach1").getDisplayName());
        assertTrue(store.get("coach1").matches("kept-password"), "an existing account is not reset");
        assertNotNull(store.get("admin1"));
        assertNotNull(store.getTeam("demo-team"));
    }

    @Test
    void anAccountAnotherClientCreatedFirstIsKeptNotOverwritten() {
        final FileAppDataAccessObject store = new FileAppDataAccessObject(
                tempDir.resolve("race.json")) {
            @Override
            public void createUser(User user) {
                if ("player1".equals(user.getUsername())) {
                    throw new use_case.accounts.DuplicateUsernameException("player1");
                }
                super.createUser(user);
            }
        };

        assertEquals("", AppBuilder.seedDemoDataIfEmpty(store, new UserFactory()));

        assertNotNull(store.get("admin1"), "the seed carries on past a name that was taken");
        assertNull(store.get("player1"));
    }

    @Test
    void anUnreachableStoreIsReportedInsteadOfThrown() {
        final FileAppDataAccessObject unreachable =
                new FileAppDataAccessObject(tempDir.resolve("unreachable.json")) {
                    @Override
                    public java.util.List<User> getAllUsers() {
                        throw new UncheckedIOException("connection refused",
                                new IOException("refused"));
                    }
                };

        final String notice = AppBuilder.seedDemoDataIfEmpty(unreachable, new UserFactory());

        assertTrue(notice.startsWith("Could not reach the account store:"), notice);
        assertTrue(notice.contains("connection refused"), notice);
        assertTrue(notice.contains("Sign-in will fail"), notice);
    }
}
