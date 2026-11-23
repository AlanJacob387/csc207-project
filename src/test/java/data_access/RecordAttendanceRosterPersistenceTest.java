package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import entity.AttendanceRecord;

/** A failed disk write must leave the in-memory roster unchanged. */
public class RecordAttendanceRosterPersistenceTest {
    @Test
    void restoresRosterWhenWriteFails(@TempDir Path tempDir) {
        final Path file = tempDir.resolve("app.json");
        final FileAppDataAccessObject initial = new FileAppDataAccessObject(file);
        initial.saveAttendanceRoster("event", List.of(
                new AttendanceRecord("event", "alice", AttendanceRecord.Status.PRESENT)));
        final FileAppDataAccessObject failing = new FileAppDataAccessObject(file) {
            @Override
            void replaceFile(Path temporary, Path target) throws IOException {
                throw new IOException("simulated disk error");
            }
        };
        assertThrows(UncheckedIOException.class, () -> failing.saveAttendanceRoster("event", List.of(
                new AttendanceRecord("event", "alice", AttendanceRecord.Status.ABSENT))));
        assertEquals(AttendanceRecord.Status.PRESENT,
                failing.getAttendanceFor("event").get(0).getStatus());
        assertEquals(AttendanceRecord.Status.PRESENT,
                new FileAppDataAccessObject(file).getAttendanceFor("event").get(0).getStatus());
    }
}
