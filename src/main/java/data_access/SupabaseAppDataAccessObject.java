package data_access;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import entity.Announcement;
import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import okhttp3.HttpUrl;
import use_case.accounts.DuplicateUsernameException;

/**
 * Hosted store: the Supabase Postgres database read and written through its REST
 * API (PostgREST).
 *
 * The project URL and the publishable key come from {@code TEAMSPORTS_DB_URL}
 * and {@code TEAMSPORTS_DB_KEY}. When either is absent, {@link #fromEnvironment}
 * returns null and AppBuilder keeps using the local JSON file, so a fresh clone
 * and the test suite need no account.
 *
 * Each table is read and written on its own, so one save touches one row. That
 * is what the local file store could not do: it rewrote the whole document, so
 * two clients saving at once lost each other's unrelated records.
 *
 * <pre>
 * read one   GET   /rest/v1/users?select=*&amp;username=eq.coach1&amp;limit=1
 * upsert     POST  /rest/v1/users?on_conflict=username
 *                  Prefer: resolution=merge-duplicates,return=minimal
 * </pre>
 *
 * Failures surface as the exceptions the interactors already handle:
 * IllegalStateException when the database answers with an error, and
 * UncheckedIOException when it cannot be reached at all.
 */
public class SupabaseAppDataAccessObject implements AppDataStore {

    /** Environment variable holding the project URL. */
    public static final String URL_VARIABLE = DatabaseConfig.URL_VARIABLE;

    /** Environment variable holding the publishable key. */
    public static final String KEY_VARIABLE = DatabaseConfig.KEY_VARIABLE;

    private static final String USERS = "users";
    private static final String TEAMS = "teams";
    private static final String EVENTS = "events";
    private static final String ATTENDANCE = "attendance";
    private static final String ANNOUNCEMENTS = "announcements";

    private final String projectUrl;
    private final PostgrestClient client;

    /**
     * Creates the store.
     *
     * @param projectUrl     the project URL, with or without the /rest/v1 suffix
     * @param publishableKey the publishable key; never the secret key
     */
    public SupabaseAppDataAccessObject(String projectUrl, String publishableKey) {
        if (projectUrl == null || projectUrl.isBlank()) {
            throw new IllegalArgumentException("A project URL is required.");
        }
        if (publishableKey == null || publishableKey.isBlank()) {
            throw new IllegalArgumentException("A publishable key is required.");
        }
        this.projectUrl = normaliseProjectUrl(projectUrl);
        this.client = new PostgrestClient(this.projectUrl, publishableKey.trim());
    }

    /**
     * Builds the store from the saved settings, the environment or the system
     * properties, whichever is configured.
     *
     * @return the store, or null when either variable is missing, so the caller
     *         can fall back to the local file
     */
    public static SupabaseAppDataAccessObject fromConfig() {
        final DatabaseConfig config = DatabaseConfig.resolve();
        return config == null ? null
                : new SupabaseAppDataAccessObject(config.getUrl(), config.getKey());
    }

    /**
     * Builds the store from explicit values, so the missing-configuration case
     * can be tested without touching the real environment.
     *
     * @param projectUrl     the project URL, possibly blank
     * @param publishableKey the publishable key, possibly blank
     * @return the store, or null when either value is missing
     */
    static SupabaseAppDataAccessObject fromValues(String projectUrl, String publishableKey) {
        if (projectUrl == null || projectUrl.isBlank()
                || publishableKey == null || publishableKey.isBlank()) {
            return null;
        }
        return new SupabaseAppDataAccessObject(projectUrl, publishableKey);
    }

    // ------------------------------------------------------------------
    // Accounts
    // ------------------------------------------------------------------

    @Override
    public User get(String username) {
        if (username == null || username.isEmpty()) {
            return null;
        }
        final JSONArray rows = client.selectWhere(USERS, "username", username, 1);
        return rows.isEmpty() ? null
                : PostgrestClient.mapRow(rows, 0, EntityJsonMapper::userFromJson, "an account");
    }

    @Override
    public boolean existsByName(String username) {
        return username != null && !username.isEmpty()
                && !client.selectWhere(USERS, "username", username, 1).isEmpty();
    }

    /**
     * Stores a new account with a strict insert.
     *
     * A plain POST uses PostgREST's strict resolution, so a username that is
     * already stored answers 409 and nothing is overwritten - the same shape as
     * the announcement insert, and the reason the create check in the interactor
     * is not the only guard.
     *
     * @throws DuplicateUsernameException when the database answers 409
     */
    @Override
    public void createUser(User user) {
        final JSONObject row = EntityJsonMapper.toJson(user);
        row.put("active", user.isActive());
        try {
            client.insert(USERS, row, "the account insert");
        }
        catch (PostgrestClient.Conflict ex) {
            throw new DuplicateUsernameException(user.getUsername());
        }
    }

    @Override
    public void save(User user) {
        final JSONObject row = EntityJsonMapper.toJson(user);
        // The mapper leaves the flag out when the account is active, but the
        // column must be spelled out here or a reactivated row would keep its
        // old value.
        row.put("active", user.isActive());
        client.upsert(USERS, "username", row);
    }

    @Override
    public List<User> getAllUsers() {
        final List<User> users = new ArrayList<>();
        final JSONArray rows = client.readAll(USERS);
        for (int i = 0; i < rows.length(); i++) {
            users.add(PostgrestClient.mapRow(rows, i, EntityJsonMapper::userFromJson, "an account"));
        }
        return users;
    }

    // ------------------------------------------------------------------
    // Teams, events and attendance
    // ------------------------------------------------------------------

    @Override
    public Team getTeam(String teamId) {
        if (teamId == null || teamId.isEmpty()) {
            return null;
        }
        final JSONArray rows = client.selectWhere(TEAMS, "teamId", teamId, 1);
        return rows.isEmpty() ? null
                : PostgrestClient.mapRow(rows, 0, EntityJsonMapper::teamFromJson, "a team");
    }

    @Override
    public void saveTeam(Team team) {
        client.upsert(TEAMS, "teamId", EntityJsonMapper.toJson(team));
    }

    @Override
    public List<Team> getAllTeams() {
        final List<Team> teams = new ArrayList<>();
        final JSONArray rows = client.readAll(TEAMS);
        for (int i = 0; i < rows.length(); i++) {
            teams.add(PostgrestClient.mapRow(rows, i, EntityJsonMapper::teamFromJson, "a team"));
        }
        return teams;
    }

    @Override
    public boolean teamExists(String teamId) {
        return teamId != null && !teamId.isEmpty()
                && !client.selectWhere(TEAMS, "teamId", teamId, 1).isEmpty();
    }

    @Override
    public void saveEvent(Event event) {
        client.upsert(EVENTS, "eventId", EntityJsonMapper.toJson(event));
    }

    @Override
    public Event getEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return null;
        }
        final JSONArray rows = client.selectWhere(EVENTS, "eventId", eventId, 1);
        return rows.isEmpty() ? null
                : PostgrestClient.mapRow(rows, 0, EntityJsonMapper::eventFromJson, "an event");
    }

    @Override
    public List<Event> getAllEvents() {
        final List<Event> events = new ArrayList<>();
        final JSONArray rows = client.readAll(EVENTS);
        for (int i = 0; i < rows.length(); i++) {
            events.add(PostgrestClient.mapRow(rows, i, EntityJsonMapper::eventFromJson, "an event"));
        }
        return events;
    }

    @Override
    public void saveAttendance(AttendanceRecord record) {
        client.upsert(ATTENDANCE, "eventId,playerUsername", EntityJsonMapper.toJson(record));
    }

    @Override
    public List<AttendanceRecord> getAttendanceFor(String eventId) {
        final List<AttendanceRecord> records = new ArrayList<>();
        if (eventId == null || eventId.isEmpty()) {
            return records;
        }
        final JSONArray rows = client.selectWhere(ATTENDANCE, "eventId", eventId, 0);
        for (int i = 0; i < rows.length(); i++) {
            records.add(PostgrestClient.mapRow(rows, i, EntityJsonMapper::attendanceFromJson,
                    "an attendance record"));
        }
        return records;
    }

    // ------------------------------------------------------------------
    // Announcements
    // ------------------------------------------------------------------

    /**
     * Inserts an announcement, refusing a duplicate id.
     *
     * A plain POST uses PostgREST's strict resolution, so a repeated primary key
     * answers 409. It is reported the way the file store reports it, instead of
     * silently replacing the existing post.
     */
    @Override
    public void saveAnnouncement(Announcement announcement) {
        try {
            client.insert(ANNOUNCEMENTS, EntityJsonMapper.toJson(announcement),
                    "the announcement insert");
        }
        catch (PostgrestClient.Conflict ex) {
            throw new IllegalStateException("An announcement already exists with id "
                    + announcement.getAnnouncementId());
        }
    }

    @Override
    public List<Announcement> getAnnouncementsForTeam(String teamId) {
        final List<Announcement> announcements = new ArrayList<>();
        if (teamId == null || teamId.isEmpty()) {
            return announcements;
        }
        final HttpUrl url = client.tableUrl(ANNOUNCEMENTS)
                .addQueryParameter("select", PostgrestClient.SELECT_ALL)
                .addQueryParameter("teamId", "eq." + teamId)
                .addQueryParameter("order", "createdAt.asc")
                .build();
        final JSONArray rows = client.get(url, "the read from announcements");
        for (int i = 0; i < rows.length(); i++) {
            announcements.add(PostgrestClient.mapRow(rows, i,
                    EntityJsonMapper::announcementFromJson, "an announcement"));
        }
        return announcements;
    }

    /**
     * Accepts the project URL with or without the REST suffix, so a value copied
     * from the Data API panel cannot turn into .../rest/v1/rest/v1/users.
     *
     * @param projectUrl the configured URL
     * @return the bare project URL
     */
    static String normaliseProjectUrl(String projectUrl) {
        return PostgrestClient.normaliseProjectUrl(projectUrl);
    }

    // ------------------------------------------------------------------
    // Status
    // ------------------------------------------------------------------

    @Override
    public String describe() {
        return "Hosted database: " + projectUrl;
    }
}
