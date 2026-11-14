package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import entity.Event;
import entity.Team;

/**
 * Tests saveEvent against the real JSON store, in particular that a failed
 * write doesn't leave an unsaved event behind in memory.
 */
class FileAppDataAccessObjectSaveEventTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 21, 20, 0);

    @TempDir
    Path tempDir;

    private static Event game(String id) {
        return new Event(id, "hawks", Event.EventType.GAME, WHEN, "Civic Arena", "Raptors");
    }

    @Test
    void savedEventSurvivesANewInstance() {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
        dao.saveTeam(new Team("hawks", "Hawks", "Basketball", "coach1"));

        dao.saveEvent(game("game-1"));

        assertEquals("Civic Arena", dao.getEvent("game-1").getLocation());
        assertEquals("Civic Arena",
                new FileAppDataAccessObject(file).getEvent("game-1").getLocation());
    }

    @Test
    void failedWriteLeavesNoUnsavedEventInMemory() throws Exception {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
        dao.saveTeam(new Team("hawks", "Hawks", "Basketball", "coach1"));

        // Swap the store file for a directory with the same name, so the next
        // write genuinely fails at the file system.
        Files.delete(file);
        Files.createDirectory(file);

        assertThrows(UncheckedIOException.class, () -> dao.saveEvent(game("unsaved")));

        assertNull(dao.getEvent("unsaved"),
                "event that was never written to disk is still in memory");
        assertTrue(dao.getAllEvents().isEmpty());
    }
}
