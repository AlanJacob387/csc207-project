package data_access;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import entity.Announcement;
import entity.AbsenceNotice;
import entity.AttendanceRecord;
import entity.Event;
import entity.EventNotification;
import entity.ParentPlayerLink;
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
    private static final String PARENT_LINKS = "parent_links";
    private static final String ABSENCE_NOTICES = "absence_notices";
    private static final String EVENT_NOTIFICATIONS = "event_notifications";

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

    /**
     * Changes one column of one account with a PATCH, so a credential the owner
     * changed meanwhile is not written back from a stale read.
     *
     * @throws IllegalStateException when no row has that username
     */
    @Override
    public void updateRole(String username, Role role) {
        patchAccount(username, new JSONObject().put("role", role.name()));
    }

    @Override
    public void updateActive(String username, boolean active) {
        patchAccount(username, new JSONObject().put("active", active));
    }

    private void patchAccount(String username, JSONObject columns) {
        final int changed = client.update(USERS, "username", username, columns,
                "the account update");
        if (changed == 0) {
            throw new IllegalStateException("No account found for " + username + ".");
        }
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

    /**
     * Saves an event and, when its schedule changed, records the change for the
     * notifications feed, the way the local file does in one write.
     *
     * Two requests cannot be one transaction through the REST API: the event is
     * written first, and if recording the notification then fails the event stays
     * saved and the caller is told the feed missed it.
     */
    @Override
    public void saveEvent(Event event) {
        final Event previous = getEvent(event.getEventId());
        final Team team = getTeam(event.getTeamId());
        final EventNotification notification = EventChangeNotifications.between(previous, event,
                team == null ? event.getTeamId() : team.getName());
        client.upsert(EVENTS, "eventId", EntityJsonMapper.toJson(event));
        if (notification != null) {
            try {
                client.insert(EVENT_NOTIFICATIONS, EntityJsonMapper.toJson(notification), "the notification insert");
            }
            catch (IllegalStateException | UncheckedIOException ex) {
                throw new IllegalStateException("The event was saved, but its notification could not be"
                        + " recorded: " + ex.getMessage(), ex);
            }
        }
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
        final HttpUrl url = client.where(client.tableUrl(ANNOUNCEMENTS)
                        .addQueryParameter("select", PostgrestClient.SELECT_ALL),
                "teamId", teamId)
                .addQueryParameter("order", "createdAt.asc")
                .build();
        final JSONArray rows = client.get(url, "the read from announcements");
        for (int i = 0; i < rows.length(); i++) {
            announcements.add(PostgrestClient.mapRow(rows, i,
                    EntityJsonMapper::announcementFromJson, "an announcement"));
        }
        return announcements;
    }

    // ------------------------------------------------------------------
    // Family links, absence notices and event notifications
    // ------------------------------------------------------------------

    @Override
    public List<ParentPlayerLink> getParentPlayerLinks() {
        final List<ParentPlayerLink> links = new ArrayList<>();
        final JSONArray rows = client.readAll(PARENT_LINKS);
        for (int i = 0; i < rows.length(); i++) {
            links.add(PostgrestClient.mapRow(rows, i,
                    EntityJsonMapper::parentPlayerLinkFromJson, "a family link"));
        }
        return links;
    }

    /** Linking twice is a no-op, as in the local file. */
    @Override
    public void saveParentPlayerLink(ParentPlayerLink link) {
        client.upsert(PARENT_LINKS, "parentUsername,playerUsername", EntityJsonMapper.toJson(link));
    }

    @Override
    public void deleteParentPlayerLink(String parentUsername, String playerUsername) {
        final java.util.Map<String, String> pair = new java.util.LinkedHashMap<>();
        pair.put("parentUsername", parentUsername);
        pair.put("playerUsername", playerUsername);
        client.delete(PARENT_LINKS, pair, "the removal of the family link");
    }

    @Override
    public List<AbsenceNotice> getAbsenceNotices() {
        final List<AbsenceNotice> notices = new ArrayList<>();
        final JSONArray rows = client.readAll(ABSENCE_NOTICES);
        for (int i = 0; i < rows.length(); i++) {
            notices.add(PostgrestClient.mapRow(rows, i,
                    EntityJsonMapper::absenceNoticeFromJson, "an absence notice"));
        }
        return notices;
    }

    /**
     * Saves a parent's notice, applying the local file's two rules: an id names one
     * absence, and a parent, child and event have one notice. The table's unique
     * constraint backs the second rule when two clients submit at once.
     */
    @Override
    public void saveAbsenceNotice(AbsenceNotice notice) {
        for (AbsenceNotice stored : getAbsenceNotices()) {
            final boolean sameAbsence = stored.getParentUsername().equals(notice.getParentUsername())
                    && stored.getPlayerUsername().equals(notice.getPlayerUsername())
                    && stored.getEventId().equals(notice.getEventId());
            if (stored.getNoticeId().equals(notice.getNoticeId()) && !sameAbsence) {
                throw new IllegalStateException("That notice identifier belongs to a different absence.");
            }
            if (!stored.getNoticeId().equals(notice.getNoticeId()) && sameAbsence) {
                throw new IllegalStateException("A notice already exists for this parent, child and event.");
            }
        }
        client.upsert(ABSENCE_NOTICES, "noticeId", EntityJsonMapper.toJson(notice));
    }

    @Override
    public List<EventNotification> getEventNotifications() {
        final List<EventNotification> notifications = new ArrayList<>();
        final HttpUrl url = client.tableUrl(EVENT_NOTIFICATIONS)
                .addQueryParameter("select", PostgrestClient.SELECT_ALL)
                .addQueryParameter("order", "createdAt.asc")
                .build();
        final JSONArray rows = client.get(url, "the read from " + EVENT_NOTIFICATIONS);
        for (int i = 0; i < rows.length(); i++) {
            notifications.add(PostgrestClient.mapRow(rows, i,
                    EntityJsonMapper::eventNotificationFromJson, "an event notification"));
        }
        return notifications;
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
