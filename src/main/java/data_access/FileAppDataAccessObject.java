package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
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
import use_case.browse_events.BrowseEventsDataAccessInterface;
import use_case.cancel_event.CancelEventDataAccessInterface;
import use_case.change_password.ChangePasswordUserDataAccessInterface;
import use_case.create_event.CreateEventDataAccessInterface;
import use_case.edit_event.EditEventDataAccessInterface;
import use_case.export_schedule.ExportScheduleDataAccessInterface;
import use_case.login.LoginUserDataAccessInterface;
import use_case.link_parent_to_player.LinkParentToPlayerDataAccessInterface;
import use_case.submit_absence_notice.SubmitAbsenceNoticeDataAccessInterface;
import use_case.view_absence_notices.ViewAbsenceNoticesDataAccessInterface;
import use_case.view_notifications.ViewNotificationsDataAccessInterface;
import use_case.view_event_weather.ViewEventWeatherDataAccessInterface;
import use_case.flag_rain_risk.FlagRainRiskDataAccessInterface;
import use_case.post_announcement.PostAnnouncementDataAccessInterface;
import use_case.signup.SignupUserDataAccessInterface;
import use_case.view_announcements.ViewAnnouncementsDataAccessInterface;
import use_case.view_event_details.ViewEventDetailsDataAccessInterface;
import use_case.view_schedule.ViewScheduleDataAccessInterface;

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
        ChangePasswordUserDataAccessInterface,
        ViewScheduleDataAccessInterface,
        ViewEventDetailsDataAccessInterface,
        EditEventDataAccessInterface,
        CancelEventDataAccessInterface,
        BrowseEventsDataAccessInterface,
        ExportScheduleDataAccessInterface,
        PostAnnouncementDataAccessInterface,
        ViewAnnouncementsDataAccessInterface,
        LinkParentToPlayerDataAccessInterface,
        SubmitAbsenceNoticeDataAccessInterface,
        ViewAbsenceNoticesDataAccessInterface,
        ViewNotificationsDataAccessInterface,
        ViewEventWeatherDataAccessInterface,
        FlagRainRiskDataAccessInterface {

    /** Default store location, relative to the working directory. */
    public static final Path DEFAULT_PATH = Path.of("data", "app-data.json");

    private static final String USERS = "users";
    private static final String TEAMS = "teams";
    private static final String EVENTS = "events";
    private static final String ATTENDANCE = "attendance";
    private static final String ANNOUNCEMENTS = "announcements";
    private static final String PARENT_LINKS = "parentLinks";
    private static final String ABSENCE_NOTICES = "absenceNotices";
    private static final String EVENT_NOTIFICATIONS = "eventNotifications";

    private final Path file;
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<String, Event> events = new LinkedHashMap<>();
    private final List<AttendanceRecord> attendance = new ArrayList<>();
    private final Map<String, Announcement> announcements = new LinkedHashMap<>();
    private final Map<List<String>, ParentPlayerLink> parentLinks = new LinkedHashMap<>();
    private final Map<String, AbsenceNotice> absenceNotices = new LinkedHashMap<>();
    private final Map<String, EventNotification> eventNotifications = new LinkedHashMap<>();
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
    public synchronized User get(String username) {
        return users.get(username);
    }

    @Override
    public synchronized boolean existsByName(String username) {
        return users.containsKey(username);
    }

    /**
     * Saves a user, replacing any stored user with the same username. Like
     * saveEvent, the user goes into the map before persist() and the previous
     * state is put back if the write fails, so a failed password change never
     * leaves the new password active in memory.
     *
     * @param user the user to save
     * @throws UncheckedIOException if the user could not be written
     */
    @Override
    public synchronized void save(User user) {
        final String username = user.getUsername();
        final User previous = users.put(username, user);
        try {
            persist();
        }
        catch (RuntimeException ex) {
            restore(users, username, previous);
            throw ex;
        }
    }

    /**
     * Updates only the profile fields of a user, leaving the stored password
     * untouched.
     *
     * @param username    the user's username
     * @param displayName the name to display
     * @param role        the user's role
     */
    public synchronized void saveProfile(String username, String displayName, Role role) {
        final User existing = users.get(username);
        final String password = existing == null ? "" : existing.getPassword();
        save(new User(username, password, displayName, role));
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
    public synchronized void saveEvent(Event event) {
        final String eventId = event.getEventId();
        final Event previous = events.get(eventId);
        final EventNotification notification = notificationFor(previous, event);
        events.put(eventId, copyEvent(event));
        if (notification != null) {
            eventNotifications.put(notification.getNotificationId(), notification);
        }
        try {
            persist();
        }
        catch (RuntimeException ex) {
            restore(events, eventId, previous);
            if (notification != null) {
                eventNotifications.remove(notification.getNotificationId());
            }
            throw ex;
        }
    }

    @Override
    public synchronized boolean teamExists(String teamId) {
        return teams.containsKey(teamId);
    }

    @Override
    public synchronized Event getEvent(String eventId) {
        final Event event = events.get(eventId);
        return event == null ? null : copyEvent(event);
    }

    /**
     * Returns every stored event.
     *
     * @return a copy of the event list
     */
    public synchronized List<Event> getAllEvents() {
        return new ArrayList<>(events.values().stream().map(FileAppDataAccessObject::copyEvent).toList());
    }

    // ------------------------------------------------------------------
    // Schedule (View Schedule use case)
    // ------------------------------------------------------------------

    @Override
    public synchronized List<String> getTeamIdsForMember(String username) {
        final List<String> teamIds = new ArrayList<>();
        final User user = users.get(username);
        if (user == null) {
            return teamIds;
        }
        final List<String> children = user != null && user.getRole() == Role.PARENT
                ? getLinkedPlayerUsernames(username) : List.of();
        for (Team team : teams.values()) {
            if (user.getRole() == Role.ADMIN
                    || user.getRole() == Role.COACH && username.equals(team.getCoachUsername())
                    || user.getRole() == Role.PLAYER && team.getPlayerUsernames().contains(username)
                    || user.getRole() == Role.PARENT
                        && children.stream().anyMatch(team.getPlayerUsernames()::contains)) {
                teamIds.add(team.getTeamId());
            }
        }
        return teamIds;
    }

    /** Grants event changes only while the stored account is this team's coach. */
    @Override
    public synchronized boolean canManageEvent(String username, String teamId) {
        final User user = users.get(username);
        final Team team = teams.get(teamId);
        return user != null && user.getRole() == Role.COACH && team != null
                && user.getUsername().equals(team.getCoachUsername());
    }

    @Override
    public synchronized List<Event> getEventsForTeams(Collection<String> teamIds,
                                         LocalDate from, LocalDate to) {
        final List<Event> matches = new ArrayList<>();
        for (Event event : events.values()) {
            if (event.getDateTime() == null || !teamIds.contains(event.getTeamId())) {
                continue;
            }
            final LocalDate date = event.getDateTime().toLocalDate();
            if (!date.isBefore(from) && !date.isAfter(to)) {
                matches.add(copyEvent(event));
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------
    // Events list (Browse Events and Export Schedule use cases)
    // ------------------------------------------------------------------

    @Override
    public synchronized List<Event> getEventsForTeams(Collection<String> teamIds) {
        final List<Event> matches = new ArrayList<>();
        for (Event event : events.values()) {
            if (teamIds.contains(event.getTeamId())) {
                matches.add(copyEvent(event));
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------
    // Event details (View Event Details use case)
    // ------------------------------------------------------------------

    @Override
    public synchronized String getTeamName(String teamId) {
        final Team team = teams.get(teamId);
        return team == null ? null : team.getName();
    }

    @Override
    public synchronized String getCoachUsername(String teamId) {
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
    public synchronized void updateEvent(Event updated) {
        saveEvent(updated);
    }

    // ------------------------------------------------------------------
    // Teams (Manage Teams use case)
    // ------------------------------------------------------------------

    /**
     * Saves a team.
     *
     * @param team the team to save
     */
    public synchronized void saveTeam(Team team) {
        final Team previous = teams.put(team.getTeamId(), copyTeam(team));
        try {
            persist();
        }
        catch (RuntimeException ex) {
            restore(teams, team.getTeamId(), previous);
            throw ex;
        }
    }

    /**
     * Looks up a team.
     *
     * @param teamId the team id
     * @return the team, or null if there is none
     */
    public synchronized Team getTeam(String teamId) {
        final Team team = teams.get(teamId);
        return team == null ? null : copyTeam(team);
    }

    /**
     * Returns every stored team.
     *
     * @return a copy of the team list
     */
    public synchronized List<Team> getAllTeams() {
        return new ArrayList<>(teams.values().stream().map(FileAppDataAccessObject::copyTeam).toList());
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
    public synchronized void saveAttendance(AttendanceRecord record) {
        final List<AttendanceRecord> previous = new ArrayList<>(attendance);
        attendance.removeIf(existing ->
                existing.getEventId().equals(record.getEventId())
                        && existing.getPlayerUsername().equals(record.getPlayerUsername()));
        attendance.add(record);
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
     * Returns every attendance record stored for an event.
     *
     * @param eventId the event id
     * @return the matching records
     */
    public synchronized List<AttendanceRecord> getAttendanceFor(String eventId) {
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
    public synchronized void saveAnnouncement(Announcement announcement) {
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
    public synchronized List<Announcement> getAnnouncementsForTeam(String teamId) {
        final List<Announcement> matches = new ArrayList<>();
        for (Announcement announcement : announcements.values()) {
            if (announcement.getTeamId().equals(teamId)) {
                matches.add(announcement);
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------
    // Parent access and communication
    // ------------------------------------------------------------------

    /** Returns account choices for the authorized parent-link use case. */
    public synchronized List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    /** Returns a snapshot of the administrator-maintained family links. */
    public synchronized List<ParentPlayerLink> getParentPlayerLinks() {
        return new ArrayList<>(parentLinks.values());
    }

    /** Returns only the players explicitly linked to this parent. */
    public synchronized List<String> getLinkedPlayerUsernames(String parentUsername) {
        return parentLinks.values().stream()
                .filter(link -> link.getParentUsername().equals(parentUsername))
                .map(ParentPlayerLink::getPlayerUsername).toList();
    }

    /** Adds a family link only after its complete store has been written. */
    public synchronized void saveParentPlayerLink(ParentPlayerLink link) {
        final List<String> key = List.of(link.getParentUsername(), link.getPlayerUsername());
        if (parentLinks.containsKey(key)) {
            return;
        }
        parentLinks.put(key, link);
        try {
            persist();
        }
        catch (RuntimeException ex) {
            parentLinks.remove(key);
            throw ex;
        }
    }

    /** Revokes one explicit link while preserving other family members. */
    public synchronized void deleteParentPlayerLink(String parentUsername, String playerUsername) {
        final List<String> key = List.of(parentUsername, playerUsername);
        final ParentPlayerLink previous = parentLinks.remove(key);
        if (previous == null) {
            return;
        }
        try {
            persist();
        }
        catch (RuntimeException ex) {
            parentLinks.put(key, previous);
            throw ex;
        }
    }

    /** Returns notices for the use case to scope to the current parent or coach. */
    public synchronized List<AbsenceNotice> getAbsenceNotices() {
        return new ArrayList<>(absenceNotices.values());
    }

    /** Updates a parent's own notice for the same child and event. */
    public synchronized void saveAbsenceNotice(AbsenceNotice notice) {
        final AbsenceNotice previous = absenceNotices.get(notice.getNoticeId());
        if (previous != null && !sameNotice(previous, notice)) {
            throw new IllegalStateException("That notice identifier belongs to a different absence.");
        }
        for (AbsenceNotice stored : absenceNotices.values()) {
            if (!stored.getNoticeId().equals(notice.getNoticeId()) && sameNotice(stored, notice)) {
                throw new IllegalStateException("A notice already exists for this parent, child and event.");
            }
        }
        absenceNotices.put(notice.getNoticeId(), notice);
        try {
            persist();
        }
        catch (RuntimeException ex) {
            restore(absenceNotices, notice.getNoticeId(), previous);
            throw ex;
        }
    }

    /** Returns the durable event-change history for authorized feed readers. */
    public synchronized List<EventNotification> getEventNotifications() {
        return new ArrayList<>(eventNotifications.values());
    }

    private static boolean sameNotice(AbsenceNotice first, AbsenceNotice second) {
        return first.getParentUsername().equals(second.getParentUsername())
                && first.getPlayerUsername().equals(second.getPlayerUsername())
                && first.getEventId().equals(second.getEventId());
    }

    private static Event copyEvent(Event event) {
        final Event copy = new Event(event.getEventId(), event.getTeamId(), event.getType(),
                event.getDateTime(), event.getLocation(), event.getOpponent());
        if (event.isCancelled()) {
            copy.cancel();
        }
        return copy;
    }

    private static Team copyTeam(Team team) {
        final Team copy = new Team(team.getTeamId(), team.getName(), team.getSport(), team.getCoachUsername());
        for (String player : team.getPlayerUsernames()) {
            copy.addPlayer(player);
        }
        return copy;
    }

    /** Distinguishes a new store from any existing accounts, records or extension data. */
    public synchronized boolean isEmpty() {
        return storedDocument.isEmpty() && users.isEmpty() && teams.isEmpty() && events.isEmpty()
                && attendance.isEmpty() && announcements.isEmpty() && parentLinks.isEmpty()
                && absenceNotices.isEmpty() && eventNotifications.isEmpty();
    }

    /** Creates the complete demo in one durable write, and leaves existing stores intact. */
    public synchronized boolean initializeDemoData(List<User> demoUsers, List<Team> demoTeams,
                                                   List<Event> demoEvents, List<ParentPlayerLink> demoLinks,
                                                   List<Announcement> demoAnnouncements,
                                                   List<AbsenceNotice> demoNotices) {
        if (!isEmpty()) {
            return false;
        }
        try {
            demoUsers.forEach(user -> users.put(user.getUsername(), user));
            demoTeams.forEach(team -> teams.put(team.getTeamId(), copyTeam(team)));
            demoLinks.forEach(link -> parentLinks.put(
                    List.of(link.getParentUsername(), link.getPlayerUsername()), link));
            demoAnnouncements.forEach(announcement -> announcements.put(announcement.getAnnouncementId(), announcement));
            demoNotices.forEach(notice -> absenceNotices.put(notice.getNoticeId(), notice));
            for (Event event : demoEvents) {
                final EventNotification notification = notificationFor(null, event);
                events.put(event.getEventId(), copyEvent(event));
                eventNotifications.put(notification.getNotificationId(), notification);
            }
            persist();
            return true;
        }
        catch (RuntimeException ex) {
            users.clear();
            teams.clear();
            events.clear();
            parentLinks.clear();
            announcements.clear();
            absenceNotices.clear();
            eventNotifications.clear();
            throw ex;
        }
    }

    private EventNotification notificationFor(Event previous, Event current) {
        if (previous != null && previous.getTeamId().equals(current.getTeamId())
                && previous.getType() == current.getType()
                && Objects.equals(previous.getDateTime(), current.getDateTime())
                && Objects.equals(previous.getLocation(), current.getLocation())
                && Objects.equals(previous.getOpponent(), current.getOpponent())
                && previous.isCancelled() == current.isCancelled()) {
            return null;
        }
        final String change = previous == null ? "CREATED"
                : !previous.isCancelled() && current.isCancelled() ? "CANCELLED" : "CHANGED";
        final String verb = previous == null ? "scheduled"
                : "CANCELLED".equals(change) ? "cancelled" : "updated";
        final Team team = teams.get(current.getTeamId());
        final String teamName = team == null ? current.getTeamId() : team.getName();
        final String message = teamName + " " + current.getType().name().toLowerCase(java.util.Locale.ROOT)
                + " " + verb + " for " + current.getDateTime() + " at " + current.getLocation() + ".";
        return new EventNotification(UUID.randomUUID().toString(), current.getEventId(), current.getTeamId(),
                change, message, LocalDateTime.now());
    }

    /** Restores the original entry when a durable write fails. */
    private static <K, V> void restore(Map<K, V> map, K key, V previous) {
        if (previous == null) {
            map.remove(key);
        }
        else {
            map.put(key, previous);
        }
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
            final JSONArray linksJson = arrayOrEmpty(root, PARENT_LINKS);
            for (int i = 0; i < linksJson.length(); i++) {
                final ParentPlayerLink link = EntityJsonMapper.parentPlayerLinkFromJson(linksJson.getJSONObject(i));
                final List<String> key = List.of(link.getParentUsername(), link.getPlayerUsername());
                if (parentLinks.putIfAbsent(key, link) != null) {
                    throw new IllegalStateException("Duplicate parent-player link in " + file);
                }
            }
            final JSONArray noticesJson = arrayOrEmpty(root, ABSENCE_NOTICES);
            for (int i = 0; i < noticesJson.length(); i++) {
                final AbsenceNotice notice = EntityJsonMapper.absenceNoticeFromJson(noticesJson.getJSONObject(i));
                if (absenceNotices.values().stream().anyMatch(existing -> sameNotice(existing, notice))) {
                    throw new IllegalStateException("Duplicate absence for the same parent, child and event in " + file);
                }
                if (absenceNotices.putIfAbsent(notice.getNoticeId(), notice) != null) {
                    throw new IllegalStateException("Duplicate absence notice in " + file);
                }
            }
            final JSONArray notificationsJson = arrayOrEmpty(root, EVENT_NOTIFICATIONS);
            for (int i = 0; i < notificationsJson.length(); i++) {
                final EventNotification notification = EntityJsonMapper.eventNotificationFromJson(
                        notificationsJson.getJSONObject(i));
                if (eventNotifications.putIfAbsent(notification.getNotificationId(), notification) != null) {
                    throw new IllegalStateException("Duplicate notification in " + file);
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

        final JSONArray linksJson = new JSONArray();
        for (ParentPlayerLink link : parentLinks.values()) {
            linksJson.put(withStoredFields(PARENT_LINKS, EntityJsonMapper.toJson(link),
                    "parentUsername", "playerUsername"));
        }
        root.put(PARENT_LINKS, linksJson);

        final JSONArray noticesJson = new JSONArray();
        for (AbsenceNotice notice : absenceNotices.values()) {
            noticesJson.put(withStoredFields(ABSENCE_NOTICES, EntityJsonMapper.toJson(notice), "noticeId"));
        }
        root.put(ABSENCE_NOTICES, noticesJson);

        final JSONArray notificationsJson = new JSONArray();
        for (EventNotification notification : eventNotifications.values()) {
            notificationsJson.put(withStoredFields(EVENT_NOTIFICATIONS, EntityJsonMapper.toJson(notification),
                    "notificationId"));
        }
        root.put(EVENT_NOTIFICATIONS, notificationsJson);

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
