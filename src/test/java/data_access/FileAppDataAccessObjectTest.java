package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;

/**
 * Tests the local JSON store: writing, reading, persistence across instances,
 * and the missing-data paths.
 */
class FileAppDataAccessObjectTest {

    @TempDir
    Path tempDir;

    @Test
    void nothingIsWrittenUntilSomethingIsSaved() {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);

        assertFalse(Files.exists(file));
        assertNull(dao.get("nobody"));
        assertFalse(dao.existsByName("nobody"));
    }

    @Test
    void savedUserIsReadableAndCreatesMissingFolders() {
        final Path file = tempDir.resolve("nested").resolve("app-data.json");
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);

        dao.save(new User("p1", "pw", "Pat", Role.PLAYER));

        assertTrue(Files.exists(file));
        assertEquals("Pat", dao.get("p1").getDisplayName());
        assertEquals(Role.PLAYER, dao.get("p1").getRole());
        assertTrue(dao.existsByName("p1"));
    }

    @Test
    void dataSurvivesANewInstance() {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject first = new FileAppDataAccessObject(file);
        first.save(new User("c1", "pw", "Coach One", Role.COACH));
        final Team team = new Team("t1", "Raptors", "Basketball", "c1");
        team.addPlayer("p1");
        first.saveTeam(team);
        first.saveEvent(new Event("e1", "t1", Event.EventType.GAME,
                LocalDateTime.of(2026, 11, 1, 18, 0), "Gym A", "Bears"));
        final AttendanceRecord record = new AttendanceRecord("e1", "p1",
                AttendanceRecord.Status.PRESENT);
        record.setStatsNotes("2 goals");
        first.saveAttendance(record);

        final FileAppDataAccessObject second = new FileAppDataAccessObject(file);

        assertEquals("Coach One", second.get("c1").getDisplayName());
        assertEquals(Role.COACH, second.get("c1").getRole());
        assertEquals("Raptors", second.getTeam("t1").getName());
        assertEquals(1, second.getTeam("t1").getPlayerUsernames().size());
        assertEquals("Bears", second.getEvent("e1").getOpponent());
        assertEquals(Event.EventType.GAME, second.getEvent("e1").getType());
        assertEquals(1, second.getAttendanceFor("e1").size());
        assertEquals("2 goals", second.getAttendanceFor("e1").get(0).getStatsNotes());
    }

    @Test
    void teamExistsOnlyForSavedTeams() {
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(
                tempDir.resolve("app-data.json"));

        assertFalse(dao.teamExists("t1"));
        dao.saveTeam(new Team("t1", "Raptors", "Basketball", "c1"));
        assertTrue(dao.teamExists("t1"));
    }

    @Test
    void savingAttendanceTwiceReplacesTheEarlierRecord() {
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(
                tempDir.resolve("app-data.json"));

        dao.saveAttendance(new AttendanceRecord("e1", "p1", AttendanceRecord.Status.ABSENT));
        dao.saveAttendance(new AttendanceRecord("e1", "p1", AttendanceRecord.Status.PRESENT));

        assertEquals(1, dao.getAttendanceFor("e1").size());
        assertEquals(AttendanceRecord.Status.PRESENT,
                dao.getAttendanceFor("e1").get(0).getStatus());
    }

    @Test
    void anEmptyFileIsTreatedAsAnEmptyStore() throws Exception {
        final Path file = tempDir.resolve("app-data.json");
        Files.writeString(file, "   ");

        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);

        assertNull(dao.get("anyone"));
    }

    @Test
    void allEventsAndTeamsAreListed() {
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(
                tempDir.resolve("app-data.json"));
        dao.saveTeam(new Team("t1", "Raptors", "Basketball", "c1"));
        dao.saveTeam(new Team("t2", "Bears", "Soccer", "c2"));
        dao.saveEvent(new Event("e1", "t1", Event.EventType.PRACTICE,
                LocalDateTime.of(2026, 11, 2, 7, 0), "Gym A", null));

        assertEquals(2, dao.getAllTeams().size());
        assertEquals(1, dao.getAllEvents().size());
    }
}
