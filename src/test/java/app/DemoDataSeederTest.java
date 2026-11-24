package app;

import static org.junit.jupiter.api.Assertions.*;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.User;

/** Checks complete demo creation and preservation of existing application data. */
class DemoDataSeederTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    Path directory;

    @Test
    void createsTheCompleteLinkedDemoAndReopensIt() {
        final Path file = directory.resolve("demo.json");
        final FileAppDataAccessObject store = new FileAppDataAccessObject(file);

        assertTrue(DemoDataSeeder.seedIfEmpty(store, CLOCK));

        final FileAppDataAccessObject reopened = new FileAppDataAccessObject(file);
        assertEquals(15, reopened.getAllUsers().size());
        assertEquals(3, reopened.getAllTeams().size());
        assertEquals(42, reopened.getAllEvents().size());
        assertEquals(42, reopened.getEventNotifications().size());
        assertEquals(8, reopened.getParentPlayerLinks().size());
        assertEquals(1, reopened.getAbsenceNotices().size());
        assertEquals(2, reopened.getLinkedPlayerUsernames("parent1").size());
        assertTrue(reopened.getAllEvents().stream().allMatch(event ->
                event.getDateTime().toLocalDate().isAfter(LocalDate.now(CLOCK))
                        && !event.getDateTime().toLocalDate().isAfter(LocalDate.now(CLOCK).plusDays(14))));
    }

    @Test
    void repeatSeedingAndExistingPartialStoresStayUntouched() throws Exception {
        final Path file = directory.resolve("existing.json");
        final FileAppDataAccessObject store = new FileAppDataAccessObject(file);
        store.save(new User("player1", "original-password", "Existing Player", Role.PLAYER));
        final String before = Files.readString(file);

        assertFalse(DemoDataSeeder.seedIfEmpty(store, CLOCK));
        new AppBuilder(store);

        assertEquals(before, Files.readString(file));
        assertEquals("original-password", store.get("player1").getPassword());
        assertNull(store.get("coach1"));
        assertThrows(IllegalStateException.class, () -> DemoDataSeeder.main(new String[]{file.toString()}));
        assertEquals(before, Files.readString(file));
    }

    @Test
    void failedInitializationDoesNotLeaveHalfADemoInMemory() throws Exception {
        final Path blocker = directory.resolve("blocked");
        Files.writeString(blocker, "Keep this file");
        final FileAppDataAccessObject store = new FileAppDataAccessObject(blocker.resolve("demo.json"));

        assertThrows(UncheckedIOException.class, () -> DemoDataSeeder.seedIfEmpty(store, CLOCK));

        assertTrue(store.isEmpty());
        assertTrue(store.getAllUsers().isEmpty());
        assertTrue(store.getEventNotifications().isEmpty());
        assertEquals("Keep this file", Files.readString(blocker));
    }
}
