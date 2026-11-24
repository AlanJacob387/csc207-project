package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import use_case.export_schedule.ScheduleWriteException;

/**
 * Tests the file writer behind the Export Schedule use case against the real
 * file system, including writes that genuinely fail.
 */
class FileScheduleWriterTest {

    @TempDir
    Path tempDir;

    private final FileScheduleWriter writer = new FileScheduleWriter();

    private long leftoverTempFiles() throws Exception {
        try (Stream<Path> files = Files.list(tempDir)) {
            return files.filter(p -> p.getFileName().toString().endsWith(".tmp")).count();
        }
    }

    @Test
    void writesContentAndReplacesAnExistingFile() throws Exception {
        final Path target = tempDir.resolve("schedule.ics");
        Files.writeString(target, "old contents");

        writer.write(target.toString(), "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n");

        assertEquals("BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n",
                Files.readString(target, StandardCharsets.UTF_8));
        assertEquals(0, leftoverTempFiles());
    }

    @Test
    void targetThatIsAFolderFailsAndLeavesItAlone() throws Exception {
        final Path folder = Files.createDirectory(tempDir.resolve("schedule.ics"));
        Files.writeString(folder.resolve("keep.txt"), "keep me");

        final ScheduleWriteException ex = assertThrows(ScheduleWriteException.class,
                () -> writer.write(folder.toString(), "BEGIN:VCALENDAR"));

        assertTrue(ex.getMessage().contains("is a folder"));
        assertTrue(Files.isDirectory(folder));
        assertEquals("keep me", Files.readString(folder.resolve("keep.txt")));
        assertEquals(0, leftoverTempFiles());
    }

    @Test
    void emptyFolderTargetIsNotReplaced() throws Exception {
        final Path folder = Files.createDirectory(tempDir.resolve("empty.ics"));

        assertThrows(ScheduleWriteException.class,
                () -> writer.write(folder.toString(), "BEGIN:VCALENDAR"));

        assertTrue(Files.isDirectory(folder));
    }

    @Test
    void missingFolderGivesReadableMessage() {
        final Path target = tempDir.resolve("no-such-folder").resolve("schedule.ics");

        final ScheduleWriteException ex = assertThrows(ScheduleWriteException.class,
                () -> writer.write(target.toString(), "BEGIN:VCALENDAR"));

        assertTrue(ex.getMessage().contains("doesn't exist"), ex.getMessage());
    }
}
