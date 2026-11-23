package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import entity.Role;
import entity.User;

/**
 * Tests save(User) against the real JSON store, in particular that a failed
 * write doesn't leave an unsaved password or user behind in memory.
 */
class FileAppDataAccessObjectSaveUserTest {

    @TempDir
    Path tempDir;

    @Test
    void savedUserSurvivesANewInstance() {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);

        dao.save(new User("coach1", "old-pw", "Coach One", Role.COACH));

        assertEquals("old-pw", dao.get("coach1").getPassword());
        assertEquals("old-pw", new FileAppDataAccessObject(file).get("coach1").getPassword());
    }

    @Test
    void failedWriteKeepsTheOriginalPassword() throws Exception {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
        dao.save(new User("coach1", "old-pw", "Coach One", Role.COACH));

        // Swap the store file for a directory with the same name, so the next
        // write genuinely fails at the file system.
        Files.delete(file);
        Files.createDirectory(file);

        assertThrows(UncheckedIOException.class,
                () -> dao.save(new User("coach1", "new-pw", "Coach One", Role.COACH)));

        assertEquals("old-pw", dao.get("coach1").getPassword(),
                "password that was never written to disk is active in memory");
    }

    @Test
    void failedWriteLeavesNoUnsavedUserInMemory() throws Exception {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
        dao.save(new User("coach1", "old-pw", "Coach One", Role.COACH));

        Files.delete(file);
        Files.createDirectory(file);

        assertThrows(UncheckedIOException.class,
                () -> dao.save(new User("player1", "pw", "Pat", Role.PLAYER)));

        assertFalse(dao.existsByName("player1"),
                "user that was never written to disk is still in memory");
    }
}
