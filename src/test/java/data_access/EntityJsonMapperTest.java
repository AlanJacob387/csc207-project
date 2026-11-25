package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;

/**
 * Round-trips every entity through the JSON mapper, so the on-disk format is
 * locked down before any store depends on it.
 */
class EntityJsonMapperTest {

    @Test
    void userRoundTrip() {
        final User user = new User("p1", "pw", "Pat", Role.PLAYER);

        final User copy = EntityJsonMapper.userFromJson(EntityJsonMapper.toJson(user));

        assertEquals("p1", copy.getUsername());
        assertEquals("pw", copy.getPassword());
        assertEquals("Pat", copy.getDisplayName());
        assertEquals(Role.PLAYER, copy.getRole());
    }

    @Test
    void teamRoundTripKeepsPlayers() {
        final Team team = new Team("t1", "Raptors", "Basketball", "c1");
        team.addPlayer("p1");
        team.addPlayer("p2");

        final Team copy = EntityJsonMapper.teamFromJson(EntityJsonMapper.toJson(team));

        assertEquals("t1", copy.getTeamId());
        assertEquals("Raptors", copy.getName());
        assertEquals("Basketball", copy.getSport());
        assertEquals("c1", copy.getCoachUsername());
        assertEquals(2, copy.getPlayerUsernames().size());
        assertTrue(copy.getPlayerUsernames().contains("p2"));
    }

    @Test
    void eventRoundTripKeepsNullOpponentAndCancelledFlag() {
        final Event practice = new Event("e1", "t1", Event.EventType.PRACTICE,
                LocalDateTime.of(2026, 11, 1, 18, 0), "Gym A", null);
        practice.cancel();

        final Event copy = EntityJsonMapper.eventFromJson(EntityJsonMapper.toJson(practice));

        assertEquals("e1", copy.getEventId());
        assertEquals(Event.EventType.PRACTICE, copy.getType());
        assertEquals(LocalDateTime.of(2026, 11, 1, 18, 0), copy.getDateTime());
        assertEquals("Gym A", copy.getLocation());
        assertNull(copy.getOpponent());
        assertTrue(copy.isCancelled());
    }

    @Test
    void gameRoundTripKeepsOpponent() {
        final Event game = new Event("e2", "t1", Event.EventType.GAME,
                LocalDateTime.of(2026, 11, 3, 9, 30), "Away Field", "Bears");

        final Event copy = EntityJsonMapper.eventFromJson(EntityJsonMapper.toJson(game));

        assertEquals("Bears", copy.getOpponent());
        assertEquals(Event.EventType.GAME, copy.getType());
    }

    @Test
    void attendanceRoundTripKeepsNotes() {
        final AttendanceRecord record = new AttendanceRecord("e1", "p1",
                AttendanceRecord.Status.EXCUSED);
        record.setStatsNotes("injured");

        final AttendanceRecord copy = EntityJsonMapper.attendanceFromJson(
                EntityJsonMapper.toJson(record));


        assertEquals("e1", copy.getEventId());
        assertEquals("p1", copy.getPlayerUsername());
        assertEquals(AttendanceRecord.Status.EXCUSED, copy.getStatus());
        assertEquals("injured", copy.getStatsNotes());
    }

    @Test
    void attendanceWithoutNotesRoundTripsAsNull() {
        final AttendanceRecord record = new AttendanceRecord("e1", "p1",
                AttendanceRecord.Status.PRESENT);

        final AttendanceRecord copy = EntityJsonMapper.attendanceFromJson(
                EntityJsonMapper.toJson(record));

        assertNull(copy.getStatsNotes());
    }

    @Test
    void unknownKeysAreIgnored() {
        final JSONObject json = EntityJsonMapper.toJson(new User("a", "b", "A", Role.ADMIN));
        json.put("futureField", "ignored");

        assertEquals(Role.ADMIN, EntityJsonMapper.userFromJson(json).getRole());
    }

    @Test
    void userRoundTripKeepsTheActiveFlag() {
        final User user = new User("p1", "pw", "Pat", Role.PLAYER, false);

        final User copy = EntityJsonMapper.userFromJson(EntityJsonMapper.toJson(user));

        assertFalse(copy.isActive());
        assertEquals("p1", copy.getUsername());
    }

    @Test
    void aUserRecordWithoutAnActiveFlagIsActive() {
        // toJson leaves the flag out for an active account, which is exactly the
        // shape an older store has, so this reads the legacy case directly.
        final JSONObject json = EntityJsonMapper.toJson(new User("p1", "pw", "Pat", Role.PLAYER));

        assertFalse(json.has("active"));
        assertTrue(EntityJsonMapper.userFromJson(json).isActive());
    }
}
