package data_access;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import entity.RsvpRecord;

/** RSVP responses survive reopen and failed writes leave old responses intact. */
public final class RsvpPersistenceTest {
    @Test void survivesRestart(@TempDir Path folder) {
        Path path = folder.resolve("store.json");
        FileAppDataAccessObject store = new FileAppDataAccessObject(path);
        store.saveRsvp(new RsvpRecord("event", "p", RsvpRecord.Response.YES));
        assertEquals(RsvpRecord.Response.YES,
                new FileAppDataAccessObject(path).getRsvpsForEvent("event").get(0).getResponse());
    }
    @Test void writeFailureRestoresOldResponse(@TempDir Path folder) {
        Path path = folder.resolve("store.json");
        FileAppDataAccessObject good = new FileAppDataAccessObject(path);
        good.saveRsvp(new RsvpRecord("event", "p", RsvpRecord.Response.YES));
        FileAppDataAccessObject failing = new FileAppDataAccessObject(path) {
            @Override void replaceFile(Path temporary, Path target) throws IOException {
                throw new IOException("simulated disk error");
            }
        };
        assertThrows(UncheckedIOException.class, () -> failing.saveRsvp(
                new RsvpRecord("event", "p", RsvpRecord.Response.NO)));
        assertEquals(RsvpRecord.Response.YES,
                failing.getRsvpsForEvent("event").get(0).getResponse());
    }
}
