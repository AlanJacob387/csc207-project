package data_access;

import java.time.LocalDateTime;

import org.json.JSONArray;
import org.json.JSONObject;

import entity.Announcement;
import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;

/**
 * Converts entities to and from JSON in one place, so every store (the local
 * JSON file today, Firebase or another API later) shares a single format and
 * the mapping can be unit tested without a disk or a network.
 *
 * <pre>
 * user       {"username":"p1","password":"pw","displayName":"Pat","role":"PLAYER"}
 * team       {"teamId":"t1","name":"Raptors","sport":"Basketball","coachUsername":"c1","playerUsernames":["p1"]}
 * event      {"eventId":"e1","teamId":"t1","type":"GAME","dateTime":"2026-11-01T18:00","location":"Gym A","opponent":"Bears","cancelled":false}
 * attendance {"eventId":"e1","playerUsername":"p1","status":"PRESENT","statsNotes":"2 goals"}
 * </pre>
 */
public final class EntityJsonMapper {

    // user keys
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DISPLAY_NAME = "displayName";
    private static final String ROLE = "role";
    private static final String ACTIVE = "active";
    // team keys
    private static final String TEAM_ID = "teamId";
    private static final String NAME = "name";
    private static final String SPORT = "sport";
    private static final String COACH_USERNAME = "coachUsername";
    private static final String PLAYER_USERNAMES = "playerUsernames";
    // event keys
    private static final String EVENT_ID = "eventId";
    private static final String TYPE = "type";
    private static final String DATE_TIME = "dateTime";
    private static final String LOCATION = "location";
    private static final String OPPONENT = "opponent";
    private static final String CANCELLED = "cancelled";
    // attendance keys
    private static final String PLAYER_USERNAME = "playerUsername";
    private static final String STATUS = "status";
    private static final String STATS_NOTES = "statsNotes";
    // announcement keys
    private static final String ANNOUNCEMENT_ID = "announcementId";
    private static final String AUTHOR_USERNAME = "authorUsername";
    private static final String MESSAGE = "message";
    private static final String CREATED_AT = "createdAt";

    private EntityJsonMapper() {
    }

    /**
     * Converts a user to JSON.
     *
     * The active flag is written only when the account is deactivated, so a
     * document written before accounts could be deactivated keeps exactly its
     * old shape and a missing flag keeps meaning "active". A store that needs
     * the flag spelled out on every row (the database) adds it explicitly.
     */
    public static JSONObject toJson(User user) {
        final JSONObject json = new JSONObject();
        json.put(USERNAME, user.getUsername());
        json.put(PASSWORD, user.getPassword());
        json.put(DISPLAY_NAME, user.getDisplayName());
        json.put(ROLE, user.getRole().name());
        if (!user.isActive()) {
            json.put(ACTIVE, false);
        }
        return json;
    }

    /**
     * Rebuilds a user from JSON.
     *
     * A record written before accounts could be deactivated has no active flag;
     * treating a missing flag as true keeps older files and older database rows
     * usable.
     */
    public static User userFromJson(JSONObject json) {
        return new User(
                json.getString(USERNAME),
                json.getString(PASSWORD),
                json.getString(DISPLAY_NAME),
                Role.valueOf(json.getString(ROLE)),
                json.optBoolean(ACTIVE, true)
        );
    }

    /** Converts a team to JSON. */
    public static JSONObject toJson(Team team) {
        final JSONObject json = new JSONObject();
        json.put(TEAM_ID, team.getTeamId());
        json.put(NAME, team.getName());
        json.put(SPORT, team.getSport());
        json.put(COACH_USERNAME, team.getCoachUsername() == null
                ? JSONObject.NULL : team.getCoachUsername());
        json.put(PLAYER_USERNAMES, new JSONArray(team.getPlayerUsernames()));
        return json;
    }

    /** Rebuilds a team from JSON. */
    public static Team teamFromJson(JSONObject json) {
        final Team team = new Team(
                json.getString(TEAM_ID),
                json.getString(NAME),
                json.getString(SPORT),
                json.isNull(COACH_USERNAME) ? null : json.getString(COACH_USERNAME)
        );
        final JSONArray players = json.optJSONArray(PLAYER_USERNAMES);
        if (players != null) {
            for (int i = 0; i < players.length(); i++) {
                team.addPlayer(players.getString(i));
            }
        }
        return team;
    }

    /** Converts an event to JSON. */
    public static JSONObject toJson(Event event) {
        final JSONObject json = new JSONObject();
        json.put(EVENT_ID, event.getEventId());
        json.put(TEAM_ID, event.getTeamId());
        json.put(TYPE, event.getType().name());
        json.put(DATE_TIME, event.getDateTime().toString());
        json.put(LOCATION, event.getLocation());
        json.put(OPPONENT, event.getOpponent() == null ? JSONObject.NULL : event.getOpponent());
        json.put(CANCELLED, event.isCancelled());
        return json;
    }

    /** Rebuilds an event from JSON. */
    public static Event eventFromJson(JSONObject json) {
        final Event event = new Event(
                json.getString(EVENT_ID),
                json.getString(TEAM_ID),
                Event.EventType.valueOf(json.getString(TYPE)),
                LocalDateTime.parse(json.getString(DATE_TIME)),
                json.getString(LOCATION),
                json.isNull(OPPONENT) ? null : json.getString(OPPONENT)
        );
        if (json.optBoolean(CANCELLED, false)) {
            event.cancel();
        }
        return event;
    }

    /** Converts an attendance record to JSON. */
    public static JSONObject toJson(AttendanceRecord record) {
        final JSONObject json = new JSONObject();
        json.put(EVENT_ID, record.getEventId());
        json.put(PLAYER_USERNAME, record.getPlayerUsername());
        json.put(STATUS, record.getStatus().name());
        json.put(STATS_NOTES, record.getStatsNotes() == null
                ? JSONObject.NULL : record.getStatsNotes());
        return json;
    }

    /** Rebuilds an attendance record from JSON. */
    public static AttendanceRecord attendanceFromJson(JSONObject json) {
        final AttendanceRecord record = new AttendanceRecord(
                json.getString(EVENT_ID),
                json.getString(PLAYER_USERNAME),
                AttendanceRecord.Status.valueOf(json.getString(STATUS))
        );
        if (!json.isNull(STATS_NOTES)) {
            record.setStatsNotes(json.getString(STATS_NOTES));
        }
        return record;
    }

    /** Converts a team announcement to JSON. */
    public static JSONObject toJson(Announcement announcement) {
        final JSONObject json = new JSONObject();
        json.put(ANNOUNCEMENT_ID, announcement.getAnnouncementId());
        json.put(TEAM_ID, announcement.getTeamId());
        json.put(AUTHOR_USERNAME, announcement.getAuthorUsername());
        json.put(MESSAGE, announcement.getMessage());
        json.put(CREATED_AT, announcement.getCreatedAt().toString());
        return json;
    }

    /** Rebuilds an announcement from its stored fields. */
    public static Announcement announcementFromJson(JSONObject json) {
        return new Announcement(
                json.getString(ANNOUNCEMENT_ID),
                json.getString(TEAM_ID),
                json.getString(AUTHOR_USERNAME),
                json.getString(MESSAGE),
                LocalDateTime.parse(json.getString(CREATED_AT))
        );
    }
}
