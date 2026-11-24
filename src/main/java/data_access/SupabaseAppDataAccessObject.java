package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
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
        return rows.isEmpty() ? null
                : mapRow(rows, 0, EntityJsonMapper::userFromJson, "an account");
    }

    @Override
    public boolean existsByName(String username) {
        return username != null && !username.isEmpty()
                && !selectWhere(USERS, "username", username, 1).isEmpty();
    }

    /**
     * Stores a new account with a strict insert.
     *
     * A plain POST uses PostgREST's strict resolution, so a username that is
     * already stored answers 409 and nothing is overwritten - the same shape as
     * the announcement insert, and the reason the create check in the interactor
     * is not the only guard.
     */
    @Override
    public void createUser(User user) {
        final JSONObject row = EntityJsonMapper.toJson(user);
        row.put("active", user.isActive());
        final Request request = request(tableUrl(USERS).build())
                .post(RequestBody.create(row.toString(), JSON))
                .header("Prefer", "return=minimal")
                .build();
        try (Response response = client.newCall(request).execute()) {
            final String text = bodyOf(response);
            if (response.code() == CONFLICT) {
                throw new IllegalStateException("An account with username "
                        + user.getUsername() + " already exists.");
            }
            if (!response.isSuccessful()) {
                throw new IllegalStateException(describe(response, text,
                        "the account insert"));
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not reach the database: " + ex.getMessage(), ex);
        }
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
            users.add(mapRow(rows, i, EntityJsonMapper::userFromJson, "an account"));
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
        final JSONArray rows = selectWhere(TEAMS, "teamId", teamId, 1);
        return rows.isEmpty() ? null
                : mapRow(rows, 0, EntityJsonMapper::teamFromJson, "a team");
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
            teams.add(mapRow(rows, i, EntityJsonMapper::teamFromJson, "a team"));
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
        return rows.isEmpty() ? null
                : mapRow(rows, 0, EntityJsonMapper::eventFromJson, "an event");
    }

    @Override
    public List<Event> getAllEvents() {
        final List<Event> events = new ArrayList<>();
        final JSONArray rows = read(EVENTS);
        for (int i = 0; i < rows.length(); i++) {
            events.add(mapRow(rows, i, EntityJsonMapper::eventFromJson, "an event"));
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
            records.add(mapRow(rows, i, EntityJsonMapper::attendanceFromJson,
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
            announcements.add(mapRow(rows, i, EntityJsonMapper::announcementFromJson,
                    "an announcement"));
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
        final String shown = preview(body);
        return "The database rejected " + what + ": HTTP " + response.code()
                + (shown.isEmpty() ? "" : " " + shown);
    }

    /**
     * Parses a response body as a JSON array.
     *
     * A paused project, a proxy or a mistyped URL can answer 200 with something
     * that is not JSON, and the parser's own exception would escape the use
     * cases, which only handle IllegalStateException and UncheckedIOException.
     * The message names the problem and shows the start of the answer.
     */
    private static JSONArray parseArray(String body) {
        if (body == null || body.isBlank()) {
            return new JSONArray();
        }
        try {
            return new JSONArray(body);
        }
        catch (JSONException ex) {
            throw new IllegalStateException("The database answered with something that is not "
                    + "JSON. Check the project URL. The answer began: " + preview(body), ex);
        }
    }

    /**
     * Maps one row, naming the record when a stored value cannot be rebuilt.
     *
     * Without this a row holding an unknown role or status would throw the
     * parser's exception out of the use case. The file store sets such a file
     * aside and starts empty; here the read stops with a message, because
     * skipping the row would quietly hide an account from the administrator page.
     *
     * @param rows   the rows read from the table
     * @param index  the row to map
     * @param mapper the mapper for that entity
     * @param what   what the row should have been, for the message
     */
    private static <T> T mapRow(JSONArray rows, int index,
                                Function<JSONObject, T> mapper, String what) {
        try {
            return mapper.apply(rows.getJSONObject(index));
        }
        catch (JSONException | IllegalArgumentException ex) {
            throw new IllegalStateException("The database holds " + what
                    + " that cannot be read: " + ex.getMessage(), ex);
        }
    }

    /** Shortens an answer or a row for a message. */
    private static String preview(String body) {
        String shown = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (shown.length() > BODY_PREVIEW) {
            shown = shown.substring(0, BODY_PREVIEW) + "...";
        }
        return shown;
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
