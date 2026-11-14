package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.AttendanceRecord;
import entity.Event;
import entity.Team;
import entity.User;
import use_case.browse_events.BrowseEventsDataAccessInterface;
import use_case.cancel_event.CancelEventDataAccessInterface;
import use_case.change_password.ChangePasswordUserDataAccessInterface;
import use_case.create_event.CreateEventDataAccessInterface;
import use_case.edit_event.EditEventDataAccessInterface;
import use_case.export_schedule.ExportScheduleDataAccessInterface;
import use_case.login.LoginUserDataAccessInterface;
import use_case.signup.SignupUserDataAccessInterface;
import use_case.view_event_details.ViewEventDetailsDataAccessInterface;
import use_case.view_schedule.ViewScheduleDataAccessInterface;

/**
 * Local JSON store for application data: users, teams, events and attendance.
 *
 * The whole document is read into memory once and rewritten whenever something
 * changes, so reads are cheap and the file always matches what the app shows.
 * The file (and its folder) are created on the first write, so a fresh clone
 * runs with no setup. Pass a temporary path in tests.
 *
 * Default location: {@code data/app-data.json} relative to the working
 * directory (git-ignored).
 */
public class FileAppDataAccessObject implements
        LoginUserDataAccessInterface,
        SignupUserDataAccessInterface,
        CreateEventDataAccessInterface,
        ChangePasswordUserDataAccessInterface,
        ViewScheduleDataAccessInterface,
        ViewEventDetailsDataAccessInterface,
        EditEventDataAccessInterface,
        CancelEventDataAccessInterface,
        BrowseEventsDataAccessInterface,
        ExportScheduleDataAccessInterface {

    /** Default store location, relative to the working directory. */
    public static final Path DEFAULT_PATH = Path.of("data", "app-data.json");

    private static final String USERS = "users";
    private static final String TEAMS = "teams";
    private static final String EVENTS = "events";
    private static final String ATTENDANCE = "attendance";

    private final Path file;
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<String, Event> events = new LinkedHashMap<>();
    private final List<AttendanceRecord> attendance = new ArrayList<>();

    /** Creates a store at {@link #DEFAULT_PATH}. */
    public FileAppDataAccessObject() {
        this(DEFAULT_PATH);
    }

    /**
     * Creates a store backed by the given file.
     *
     * @param file the JSON file to read from and write to
     */
    public FileAppDataAccessObject(Path file) {
        this.file = file;
        load();
    }

    // ------------------------------------------------------------------
    // Login / signup
    // ------------------------------------------------------------------

    @Override
    public User get(String username) {
        return users.get(username);
    }

    @Override
    public boolean existsByName(String username) {
        return users.containsKey(username);
    }

    @Override
    public void save(User user) {
        users.put(user.getUsername(), user);
        persist();
    }

    // ------------------------------------------------------------------
    // Events (Create Event use case)
    // ------------------------------------------------------------------

    /**
     * Saves an event, replacing any stored event with the same id. persist()
     * writes whatever is in the map, so the event goes in first; if the write
     * fails, the previous state is put back so memory still matches the file.
     *
     * @param event the event to save
     * @throws UncheckedIOException if the event could not be written
     */
    @Override
    public void saveEvent(Event event) {
        final String eventId = event.getEventId();
        final Event previous = events.put(eventId, event);
        try {
            persist();
        }
        catch (RuntimeException ex) {
            if (previous == null) {
                events.remove(eventId);
            }
            else {
                events.put(eventId, previous);
            }
            throw ex;
        }
    }

    @Override
    public boolean teamExists(String teamId) {
        return teams.containsKey(teamId);
    }

    @Override
    public Event getEvent(String eventId) {
        return events.get(eventId);
    }

    /**
     * Returns every stored event.
     *
     * @return a copy of the event list
     */
    public List<Event> getAllEvents() {
        return new ArrayList<>(events.values());
    }

    // ------------------------------------------------------------------
    // Schedule (View Schedule use case)
    // ------------------------------------------------------------------

    @Override
    public List<String> getTeamIdsForMember(String username) {
        final List<String> teamIds = new ArrayList<>();
        for (Team team : teams.values()) {
            if (username.equals(team.getCoachUsername())
                    || team.getPlayerUsernames().contains(username)) {
                teamIds.add(team.getTeamId());
            }
        }
        return teamIds;
    }

    @Override
    public List<Event> getEventsForTeams(Collection<String> teamIds,
                                         LocalDate from, LocalDate to) {
        final List<Event> matches = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getDateTime() == null || !teamIds.contains(event.getTeamId())) {
                continue;
            }
            final LocalDate date = event.getDateTime().toLocalDate();
            if (!date.isBefore(from) && !date.isAfter(to)) {
                matches.add(event);
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------
    // Events list (Browse Events and Export Schedule use cases)
    // ------------------------------------------------------------------

    @Override
    public List<Event> getEventsForTeams(Collection<String> teamIds) {
        final List<Event> matches = new ArrayList<>();
        for (Event event : events.values()) {
            if (teamIds.contains(event.getTeamId())) {
                matches.add(event);
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------
    // Event details (View Event Details use case)
    // ------------------------------------------------------------------

    @Override
    public String getTeamName(String teamId) {
        final Team team = teams.get(teamId);
        return team == null ? null : team.getName();
    }

    @Override
    public String getCoachUsername(String teamId) {
        final Team team = teams.get(teamId);
        return team == null ? null : team.getCoachUsername();
    }

    // ------------------------------------------------------------------
    // Event changes (Edit Event and Cancel Event use cases)
    // ------------------------------------------------------------------

    /**
     * Replaces the stored event with the same id. persist() writes whatever is
     * in the map, so the new event goes in first; if the write fails, the
     * previous event is put back so memory still matches the file.
     *
     * @param updated the changed event
     * @throws UncheckedIOException if the change could not be written
     */
    @Override
    public void updateEvent(Event updated) {
        final String eventId = updated.getEventId();
        final Event previous = events.put(eventId, updated);
        try {
            persist();
        }
        catch (RuntimeException ex) {
            if (previous == null) {
                events.remove(eventId);
            }
            else {
                events.put(eventId, previous);
            }
            throw ex;
        }
    }

    // ------------------------------------------------------------------
    // Teams (Manage Teams use case)
    // ------------------------------------------------------------------

    /**
     * Saves a team.
     *
     * @param team the team to save
     */
    public void saveTeam(Team team) {
        teams.put(team.getTeamId(), team);
        persist();
    }

    /**
     * Looks up a team.
     *
     * @param teamId the team id
     * @return the team, or null if there is none
     */
    public Team getTeam(String teamId) {
        return teams.get(teamId);
    }

    /**
     * Returns every stored team.
     *
     * @return a copy of the team list
     */
    public List<Team> getAllTeams() {
        return new ArrayList<>(teams.values());
    }

    // ------------------------------------------------------------------
    // Attendance (Record Attendance use case)
    // ------------------------------------------------------------------

    /**
     * Saves one player's attendance for one event, replacing any earlier record
     * for the same player and event.
     *
     * @param record the record to save
     */
    public void saveAttendance(AttendanceRecord record) {
        attendance.removeIf(existing ->
                existing.getEventId().equals(record.getEventId())
                        && existing.getPlayerUsername().equals(record.getPlayerUsername()));
        attendance.add(record);
        persist();
    }

    /**
     * Returns every attendance record stored for an event.
     *
     * @param eventId the event id
     * @return the matching records
     */
    public List<AttendanceRecord> getAttendanceFor(String eventId) {
        final List<AttendanceRecord> matches = new ArrayList<>();
        for (AttendanceRecord record : attendance) {
            if (record.getEventId().equals(eventId)) {
                matches.add(record);
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------
    // File handling
    // ------------------------------------------------------------------

    /** Reads the JSON file into memory. A missing file means an empty store. */
    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try {
            final String content = Files.readString(file, StandardCharsets.UTF_8);
            if (content.isBlank()) {
                return;
            }
            final JSONObject root = new JSONObject(content);

            final JSONArray usersJson = arrayOrEmpty(root, USERS);
            for (int i = 0; i < usersJson.length(); i++) {
                final User user = EntityJsonMapper.userFromJson(usersJson.getJSONObject(i));
                users.put(user.getUsername(), user);
            }

            final JSONArray teamsJson = arrayOrEmpty(root, TEAMS);
            for (int i = 0; i < teamsJson.length(); i++) {
                final Team team = EntityJsonMapper.teamFromJson(teamsJson.getJSONObject(i));
                teams.put(team.getTeamId(), team);
            }

            final JSONArray eventsJson = arrayOrEmpty(root, EVENTS);
            for (int i = 0; i < eventsJson.length(); i++) {
                final Event event = EntityJsonMapper.eventFromJson(eventsJson.getJSONObject(i));
                events.put(event.getEventId(), event);
            }

            final JSONArray attendanceJson = arrayOrEmpty(root, ATTENDANCE);
            for (int i = 0; i < attendanceJson.length(); i++) {
                attendance.add(EntityJsonMapper.attendanceFromJson(
                        attendanceJson.getJSONObject(i)));
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not read app data from " + file, ex);
        }
        catch (JSONException ex) {
            throw new IllegalStateException("App data file is not valid JSON: " + file, ex);
        }
    }

    /** Writes the whole store back to disk, creating folders if needed. */
    private void persist() {
        final JSONObject root = new JSONObject();

        final JSONArray usersJson = new JSONArray();
        for (User user : users.values()) {
            usersJson.put(EntityJsonMapper.toJson(user));
        }
        root.put(USERS, usersJson);

        final JSONArray teamsJson = new JSONArray();
        for (Team team : teams.values()) {
            teamsJson.put(EntityJsonMapper.toJson(team));
        }
        root.put(TEAMS, teamsJson);

        final JSONArray eventsJson = new JSONArray();
        for (Event event : events.values()) {
            eventsJson.put(EntityJsonMapper.toJson(event));
        }
        root.put(EVENTS, eventsJson);

        final JSONArray attendanceJson = new JSONArray();
        for (AttendanceRecord record : attendance) {
            attendanceJson.put(EntityJsonMapper.toJson(record));
        }
        root.put(ATTENDANCE, attendanceJson);

        try {
            final Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, root.toString(2), StandardCharsets.UTF_8);
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not write app data to " + file, ex);
        }
    }

    /** Reads a top-level array, treating a missing key as empty. */
    private static JSONArray arrayOrEmpty(JSONObject root, String key) {
        final JSONArray array = root.optJSONArray(key);
        return array == null ? new JSONArray() : array;
    }
}
