package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entity.Announcement;
import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.create_event.CreateEventDataAccessInterface;
import use_case.record_attendance.RecordAttendanceDataAccessInterface;
import use_case.login.LoginUserDataAccessInterface;
import use_case.post_announcement.PostAnnouncementDataAccessInterface;
import use_case.signup.SignupUserDataAccessInterface;
import use_case.view_announcements.ViewAnnouncementsDataAccessInterface;

/**
 * Local JSON store for users, teams, events, attendance and announcements.
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
        RecordAttendanceDataAccessInterface,
        PostAnnouncementDataAccessInterface,
        ViewAnnouncementsDataAccessInterface {

    /** Default store location, relative to the working directory. */
    public static final Path DEFAULT_PATH = Path.of("data", "app-data.json");

    private static final String USERS = "users";
    private static final String TEAMS = "teams";
    private static final String EVENTS = "events";
    private static final String ATTENDANCE = "attendance";
    private static final String ANNOUNCEMENTS = "announcements";

    private final Path file;
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<String, Event> events = new LinkedHashMap<>();
    private final List<AttendanceRecord> attendance = new ArrayList<>();
    private final Map<String, Announcement> announcements = new LinkedHashMap<>();
    private JSONObject storedDocument = new JSONObject();

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

    /**
     * Updates only the profile fields of a user, leaving the stored password
     * untouched. Useful when credentials are kept somewhere else, such as a hosted store.
     *
     * @param username    the user's username
     * @param displayName the name to display
     * @param role        the user's role
     */
    public void saveProfile(String username, String displayName, Role role) {
        final User existing = users.get(username);
        final String password = existing == null ? "" : existing.getPassword();
        users.put(username, new User(username, password, displayName, role));
        persist();
    }

    // ------------------------------------------------------------------
    // Events (Create Event use case)
    // ------------------------------------------------------------------

    @Override
    public void saveEvent(Event event) {
        events.put(event.getEventId(), event);
        persist();
    }

    @Override
    public boolean teamExists(String teamId) {
        return teams.containsKey(teamId);
    }

    /**
     * Looks up an event.
     *
     * @param eventId the event id
     * @return the event, or null if there is none
     */
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

    /** Atomically stores one event roster; no partially committed player updates. */
    @Override
    public void saveAttendanceRoster(String eventId, List<AttendanceRecord> records) {
        final List<AttendanceRecord> previous = new ArrayList<>(attendance);
        attendance.removeIf(record -> eventId.equals(record.getEventId()));
        attendance.addAll(records);
        try {
            persist();
        }
        catch (RuntimeException ex) {
            attendance.clear();
            attendance.addAll(previous);
            throw ex;
        }
    }

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
    // Announcements
    // ------------------------------------------------------------------

    /** Saves a new announcement without replacing an existing announcement. */
    @Override
    public void saveAnnouncement(Announcement announcement) {
        final String announcementId = announcement.getAnnouncementId();
        if (announcements.containsKey(announcementId)) {
            throw new IllegalStateException("An announcement already exists with id " + announcementId);
        }
        final Map<String, Announcement> updated = new LinkedHashMap<>(announcements);
        updated.put(announcementId, announcement);
        persist(updated);
        announcements.put(announcementId, announcement);
    }

    /** Returns a separate list containing only the requested team's announcements. */
    @Override
    public List<Announcement> getAnnouncementsForTeam(String teamId) {
        final List<Announcement> matches = new ArrayList<>();
        for (Announcement announcement : announcements.values()) {
            if (announcement.getTeamId().equals(teamId)) {
                matches.add(announcement);
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

            final JSONArray announcementsJson = arrayOrEmpty(root, ANNOUNCEMENTS);
            for (int i = 0; i < announcementsJson.length(); i++) {
                final Announcement announcement = EntityJsonMapper.announcementFromJson(
                        announcementsJson.getJSONObject(i));
                if (announcements.putIfAbsent(announcement.getAnnouncementId(), announcement) != null) {
                    throw new IllegalStateException("Duplicate announcement id in " + file);
                }
            }
            storedDocument = root;
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not read app data from " + file, ex);
        }
        catch (JSONException | IllegalArgumentException | DateTimeException ex) {
            throw new IllegalStateException("App data file has an invalid format: " + file, ex);
        }
    }

    /** Writes the whole store back to disk, creating folders if needed. */
    private void persist() {
        persist(announcements);
    }

    /** Writes a staged announcement collection alongside the current legacy data. */
    private void persist(Map<String, Announcement> announcementsToPersist) {
        final JSONObject root = new JSONObject(storedDocument.toString());

        final JSONArray usersJson = new JSONArray();
        for (User user : users.values()) {
            usersJson.put(withStoredFields(USERS, EntityJsonMapper.toJson(user), "username"));
        }
        root.put(USERS, usersJson);

        final JSONArray teamsJson = new JSONArray();
        for (Team team : teams.values()) {
            teamsJson.put(withStoredFields(TEAMS, EntityJsonMapper.toJson(team), "teamId"));
        }
        root.put(TEAMS, teamsJson);

        final JSONArray eventsJson = new JSONArray();
        for (Event event : events.values()) {
            eventsJson.put(withStoredFields(EVENTS, EntityJsonMapper.toJson(event), "eventId"));
        }
        root.put(EVENTS, eventsJson);

        final JSONArray attendanceJson = new JSONArray();
        for (AttendanceRecord record : attendance) {
            attendanceJson.put(withStoredFields(ATTENDANCE, EntityJsonMapper.toJson(record),
                    "eventId", "playerUsername"));
        }
        root.put(ATTENDANCE, attendanceJson);

        final JSONArray announcementsJson = new JSONArray();
        for (Announcement announcement : announcementsToPersist.values()) {
            announcementsJson.put(withStoredFields(ANNOUNCEMENTS, EntityJsonMapper.toJson(announcement),
                    "announcementId"));
        }
        root.put(ANNOUNCEMENTS, announcementsJson);

        try {
            writeAtomically(root.toString(2));
            storedDocument = root;
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not write app data to " + file, ex);
        }
    }

    /** Keeps unrecognised record fields when a different feature updates the store. */
    private JSONObject withStoredFields(String section, JSONObject current, String... identityKeys) {
        final JSONArray storedRecords = arrayOrEmpty(storedDocument, section);
        for (int i = 0; i < storedRecords.length(); i++) {
            final JSONObject stored = storedRecords.getJSONObject(i);
            boolean matches = true;
            for (String key : identityKeys) {
                matches = matches && current.get(key).equals(stored.opt(key));
            }
            if (matches) {
                final JSONObject merged = new JSONObject(stored.toString());
                for (String key : current.keySet()) {
                    merged.put(key, current.get(key));
                }
                return merged;
            }
        }
        return current;
    }

    /** Writes a complete sibling file before replacing the previous store. */
    private void writeAtomically(String content) throws IOException {
        final Path target = file.toAbsolutePath();
        final Path parent = target.getParent();
        Files.createDirectories(parent);
        final Path temporary = Files.createTempFile(parent, "." + target.getFileName() + "-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            replaceFile(temporary, target);
        }
        catch (IOException ex) {
            try {
                Files.deleteIfExists(temporary);
            }
            catch (IOException cleanupFailure) {
                ex.addSuppressed(cleanupFailure);
            }
            throw ex;
        }
    }

    /** Replaces the store after its complete contents have been written. */
    void replaceFile(Path temporary, Path target) throws IOException {
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        }
        catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Reads a top-level array, treating a missing key as empty. */
    private static JSONArray arrayOrEmpty(JSONObject root, String key) {
        return root.has(key) ? root.getJSONArray(key) : new JSONArray();
    }
}
