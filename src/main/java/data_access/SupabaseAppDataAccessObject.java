package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import entity.Announcement;
import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

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

    private static final String REST_PATH = "/rest/v1";
    private static final String SELECT_ALL = "*";
    private static final int CONFLICT = 409;
    private static final int BODY_PREVIEW = 240;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String projectUrl;
    private final String apiKey;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

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
        this.apiKey = publishableKey.trim();
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
        final JSONArray rows = selectWhere(USERS, "username", username, 1);
        return rows.isEmpty() ? null : EntityJsonMapper.userFromJson(rows.getJSONObject(0));
    }

    @Override
    public boolean existsByName(String username) {
        return username != null && !username.isEmpty()
                && !selectWhere(USERS, "username", username, 1).isEmpty();
    }

    @Override
    public void save(User user) {
        final JSONObject row = EntityJsonMapper.toJson(user);
        // The mapper leaves the flag out when the account is active, but the
        // column must be spelled out here or a reactivated row would keep its
        // old value.
        row.put("active", user.isActive());
        upsert(USERS, "username", row);
    }

    @Override
    public List<User> getAllUsers() {
        final List<User> users = new ArrayList<>();
        final JSONArray rows = read(USERS);
        for (int i = 0; i < rows.length(); i++) {
            users.add(EntityJsonMapper.userFromJson(rows.getJSONObject(i)));
        }
        return users;
    }

    @Override
    public void saveProfile(String username, String displayName, Role role) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("A username is required.");
        }
        final JSONObject patch = new JSONObject();
        patch.put("displayName", displayName);
        patch.put("role", role == null ? JSONObject.NULL : role.name());

        final HttpUrl url = tableUrl(USERS).addQueryParameter("username", "eq." + username).build();
        execute(request(url)
                .patch(RequestBody.create(patch.toString(), JSON))
                .header("Prefer", "return=minimal")
                .build(), "the profile update for " + username);
    }

    // ------------------------------------------------------------------
    // Teams, events and attendance
    // ------------------------------------------------------------------

    @Override
    public Team getTeam(String teamId) {
        if (teamId == null || teamId.isEmpty()) {
            return null;
        }
        final JSONArray rows = selectWhere(TEAMS, "teamId", teamId, 1);
        return rows.isEmpty() ? null : EntityJsonMapper.teamFromJson(rows.getJSONObject(0));
    }

    @Override
    public void saveTeam(Team team) {
        upsert(TEAMS, "teamId", EntityJsonMapper.toJson(team));
    }

    @Override
    public List<Team> getAllTeams() {
        final List<Team> teams = new ArrayList<>();
        final JSONArray rows = read(TEAMS);
        for (int i = 0; i < rows.length(); i++) {
            teams.add(EntityJsonMapper.teamFromJson(rows.getJSONObject(i)));
        }
        return teams;
    }

    @Override
    public boolean teamExists(String teamId) {
        return teamId != null && !teamId.isEmpty()
                && !selectWhere(TEAMS, "teamId", teamId, 1).isEmpty();
    }

    @Override
    public void saveEvent(Event event) {
        upsert(EVENTS, "eventId", EntityJsonMapper.toJson(event));
    }

    @Override
    public Event getEvent(String eventId) {
        if (eventId == null || eventId.isEmpty()) {
            return null;
        }
        final JSONArray rows = selectWhere(EVENTS, "eventId", eventId, 1);
        return rows.isEmpty() ? null : EntityJsonMapper.eventFromJson(rows.getJSONObject(0));
    }

    @Override
    public List<Event> getAllEvents() {
        final List<Event> events = new ArrayList<>();
        final JSONArray rows = read(EVENTS);
        for (int i = 0; i < rows.length(); i++) {
            events.add(EntityJsonMapper.eventFromJson(rows.getJSONObject(i)));
        }
        return events;
    }

    @Override
    public void saveAttendance(AttendanceRecord record) {
        upsert(ATTENDANCE, "eventId,playerUsername", EntityJsonMapper.toJson(record));
    }

    @Override
    public List<AttendanceRecord> getAttendanceFor(String eventId) {
        final List<AttendanceRecord> records = new ArrayList<>();
        if (eventId == null || eventId.isEmpty()) {
            return records;
        }
        final JSONArray rows = selectWhere(ATTENDANCE, "eventId", eventId, 0);
        for (int i = 0; i < rows.length(); i++) {
            records.add(EntityJsonMapper.attendanceFromJson(rows.getJSONObject(i)));
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
        final Request request = request(tableUrl(ANNOUNCEMENTS).build())
                .post(RequestBody.create(EntityJsonMapper.toJson(announcement).toString(), JSON))
                .header("Prefer", "return=minimal")
                .build();
        try (Response response = client.newCall(request).execute()) {
            final String text = bodyOf(response);
            if (response.code() == CONFLICT) {
                throw new IllegalStateException("An announcement already exists with id "
                        + announcement.getAnnouncementId());
            }
            if (!response.isSuccessful()) {
                throw new IllegalStateException(describe(response, text,
                        "the announcement insert"));
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not reach the database: " + ex.getMessage(), ex);
        }
    }

    @Override
    public List<Announcement> getAnnouncementsForTeam(String teamId) {
        final List<Announcement> announcements = new ArrayList<>();
        if (teamId == null || teamId.isEmpty()) {
            return announcements;
        }
        final HttpUrl url = tableUrl(ANNOUNCEMENTS)
                .addQueryParameter("select", SELECT_ALL)
                .addQueryParameter("teamId", "eq." + teamId)
                .addQueryParameter("order", "createdAt.asc")
                .build();
        final JSONArray rows = parseArray(execute(request(url).get().build(),
                "the read from announcements"));
        for (int i = 0; i < rows.length(); i++) {
            announcements.add(EntityJsonMapper.announcementFromJson(rows.getJSONObject(i)));
        }
        return announcements;
    }

    // ------------------------------------------------------------------
    // Request helpers
    // ------------------------------------------------------------------

    /** Reads every row of a table. */
    private JSONArray read(String table) {
        final HttpUrl url = tableUrl(table).addQueryParameter("select", SELECT_ALL).build();
        return parseArray(execute(request(url).get().build(), "the read from " + table));
    }

    /**
     * Reads the rows where one column has a value.
     *
     * @param limit the maximum number of rows, or 0 for no limit
     */
    private JSONArray selectWhere(String table, String column, String value, int limit) {
        HttpUrl.Builder url = tableUrl(table)
                .addQueryParameter("select", SELECT_ALL)
                .addQueryParameter(column, "eq." + value);
        if (limit > 0) {
            url = url.addQueryParameter("limit", Integer.toString(limit));
        }
        return parseArray(execute(request(url.build()).get().build(), "the read from " + table));
    }

    /** Builds the route for one table. */
    private HttpUrl.Builder tableUrl(String table) {
        return HttpUrl.get(projectUrl + REST_PATH + "/" + table).newBuilder();
    }

    /** Adds the publishable key and the JSON accept header. */
    private Request.Builder request(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Accept", "application/json");
    }

    /** Inserts a row, or replaces the row with the same conflict columns. */
    private void upsert(String table, String conflictColumns, JSONObject row) {
        final HttpUrl url = tableUrl(table)
                .addQueryParameter("on_conflict", conflictColumns)
                .build();
        execute(request(url)
                .post(RequestBody.create(row.toString(), JSON))
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .build(), "the write to " + table);
    }

    private String execute(Request request, String what) {
        try (Response response = client.newCall(request).execute()) {
            final String text = bodyOf(response);
            if (!response.isSuccessful()) {
                throw new IllegalStateException(describe(response, text, what));
            }
            return text;
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not reach the database: " + ex.getMessage(), ex);
        }
    }

    private static String bodyOf(Response response) throws IOException {
        final ResponseBody body = response.body();
        return body == null ? "" : body.string();
    }

    private static String describe(Response response, String body, String what) {
        String preview = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (preview.length() > BODY_PREVIEW) {
            preview = preview.substring(0, BODY_PREVIEW) + "...";
        }
        return "The database rejected " + what + ": HTTP " + response.code()
                + (preview.isEmpty() ? "" : " " + preview);
    }

    private static JSONArray parseArray(String body) {
        return body == null || body.isBlank() ? new JSONArray() : new JSONArray(body);
    }

    /**
     * Accepts the project URL with or without the REST suffix, so a value copied
     * from the Data API panel cannot turn into .../rest/v1/rest/v1/users.
     *
     * @param projectUrl the configured URL
     * @return the bare project URL
     */
    static String normaliseProjectUrl(String projectUrl) {
        String url = projectUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (url.endsWith(REST_PATH)) {
            url = url.substring(0, url.length() - REST_PATH.length());
        }
        return url;
    }

    // ------------------------------------------------------------------
    // Status and connection test, for the settings page
    // ------------------------------------------------------------------

    @Override
    public String describe() {
        return "Hosted database: " + projectUrl;
    }

    /**
     * Tries the given settings against the database, so the settings page can
     * tell the user whether they work before saving them.
     *
     * @param projectUrl the project URL
     * @param apiKey     the publishable key
     * @return the outcome, ready to show
     */
    public static ConnectionCheck testConnection(String projectUrl, String apiKey) {
        try {
            final DatabaseConfig config = DatabaseConfig.fromValues(projectUrl, apiKey,
                    "settings page");
            if (config == null) {
                return new ConnectionCheck(false, "Enter both a URL and a publishable key.");
            }
            final SupabaseAppDataAccessObject store =
                    new SupabaseAppDataAccessObject(config.getUrl(), config.getKey());
            final int accounts = store.getAllUsers().size();
            return new ConnectionCheck(true, "Connected: " + accounts + " account(s) stored.");
        }
        catch (RuntimeException ex) {
            return new ConnectionCheck(false, "Could not connect: " + ex.getMessage());
        }
    }

    /** The outcome of a connection test. */
    public static final class ConnectionCheck {

        private final boolean ok;
        private final String message;

        /**
         * Creates the result.
         *
         * @param ok      whether the settings worked
         * @param message what to show the user
         */
        public ConnectionCheck(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }
    }
}
