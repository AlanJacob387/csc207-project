package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
 * Tests updateEvent against the real JSON store, in particular that a failed
 * write leaves the in-memory event exactly as it was.
 */
class FileAppDataAccessObjectUpdateEventTest {

    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 11, 21, 20, 0);

    @TempDir
    Path tempDir;

    private static Event game() {
        return new Event("game-1", "hawks", Event.EventType.GAME, WHEN, "Civic Arena", "Raptors");
    }

    private static Event cancelledCopy(Event event) {
        final Event copy = new Event(event.getEventId(), event.getTeamId(), event.getType(),
                event.getDateTime(), event.getLocation(), event.getOpponent());
        copy.cancel();
        return copy;
    }

    @Test
    void updateIsSavedAndSurvivesANewInstance() {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
        dao.saveTeam(new Team("hawks", "Hawks", "Basketball", "coach1"));
        dao.saveEvent(game());

        dao.updateEvent(cancelledCopy(dao.getEvent("game-1")));

        assertTrue(dao.getEvent("game-1").isCancelled());
        assertTrue(new FileAppDataAccessObject(file).getEvent("game-1").isCancelled());
    }

    @Test
    void failedWriteRollsBackToTheOriginalEvent() throws Exception {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
        dao.saveTeam(new Team("hawks", "Hawks", "Basketball", "coach1"));
        dao.saveEvent(game());
        final Event original = dao.getEvent("game-1");

        // Swap the store file for a directory with the same name, so the next
        // write genuinely fails at the file system.
        Files.delete(file);
        Files.createDirectory(file);

        final Event changed = cancelledCopy(original);
        changed.setLocation("Somewhere Else");
        assertThrows(UncheckedIOException.class, () -> dao.updateEvent(changed));

        final Event afterwards = dao.getEvent("game-1");
        assertSame(original, afterwards);
        assertFalse(afterwards.isCancelled());
        assertEquals("Civic Arena", afterwards.getLocation());
        assertEquals(WHEN, afterwards.getDateTime());
    }
}
