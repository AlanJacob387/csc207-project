package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import entity.Announcement;
import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.User;
import use_case.accounts.DuplicateUsernameException;

/**
 * Tests the hosted store against a local stub server, so every request shape is
 * verified without an account, a network or a bill.
 *
 * The stub records each request and the test asserts what the store sent:
 * method, route, filter, headers and body. Any status code can be forced back.
 */
class SupabaseAppDataAccessObjectTest {

    /** One request the store sent. */
    private static class Recorded {
        private String method;
        private String path;
        private String query;
        private String rawQuery;
        private String apikey;
        private String prefer;
        private String body;
    }

    private static final String USER_ROW =
            "{\"username\":\"coach1\",\"password\":\"pbkdf2$1$c2FsdA==$aGFzaA==\","
                    + "\"displayName\":\"Coach One\",\"role\":\"COACH\",\"active\":true}";

    private HttpServer server;
    private final List<Recorded> recorded = new ArrayList<>();
    private int responseStatus;
    private String responseBody;
    /** Answers for particular routes, for a test that reads one table and writes another. */
    private final java.util.Map<String, String> routeBodies = new java.util.HashMap<>();
    private final java.util.Map<String, Integer> routeStatuses = new java.util.HashMap<>();
    private SupabaseAppDataAccessObject store;

    @BeforeEach
    void startStub() throws IOException {
        responseStatus = 200;
        responseBody = "[]";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", this::handle);
        server.start();
        store = new SupabaseAppDataAccessObject(
                "http://127.0.0.1:" + server.getAddress().getPort(), "sb_publishable_test");
    }

    @AfterEach
    void stopStub() {
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        final Recorded call = new Recorded();
        call.method = exchange.getRequestMethod();
        call.path = exchange.getRequestURI().getPath();
        call.query = exchange.getRequestURI().getQuery();
        call.rawQuery = exchange.getRequestURI().getRawQuery();
        call.apikey = exchange.getRequestHeaders().getFirst("apikey");
        call.prefer = exchange.getRequestHeaders().getFirst("Prefer");
        call.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        recorded.add(call);

        final byte[] bytes = routeBodies.getOrDefault(call.path, responseBody)
                .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(routeStatuses.getOrDefault(call.path, responseStatus),
                bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }
        exchange.close();
    }

    private void respond(int status, String body) {
        this.responseStatus = status;
        this.responseBody = body;
    }

    private Recorded only() {
        assertEquals(1, recorded.size(), "the store should have sent exactly one request");
        return recorded.get(0);
    }

    // ------------------------------------------------------------------
    // Accounts
    // ------------------------------------------------------------------

    @Test
    void readingAUserFiltersOnTheUsernameAndSendsTheKey() {
        respond(200, "[" + USER_ROW + "]");

        final User user = store.get("coach1");

        assertEquals("Coach One", user.getDisplayName());
        assertEquals(Role.COACH, user.getRole());
        assertTrue(user.isActive());
        final Recorded call = only();
        assertEquals("GET", call.method);
        assertEquals("/rest/v1/users", call.path);
        assertEquals("select=*&username=eq.coach1&limit=1", call.query);
        assertEquals("sb_publishable_test", call.apikey);
    }

    @Test
    void anUnknownOrBlankUsernameComesBackAsNull() {
        assertNull(store.get("ghost"));
        assertEquals("/rest/v1/users", only().path);

        assertNull(store.get(""));
        assertEquals(1, recorded.size(), "a blank username should not reach the database");
        assertFalse(store.existsByName(""));
    }

    @Test
    void aDottedUsernameIsSentAsItIsNotQuoted() {
        assertNull(store.get("pat.smith"));

        assertEquals("select=*&username=eq.pat.smith&limit=1", only().rawQuery,
                "an eq filter takes everything after the operator, so a dot needs no quotes; "
                        + "quotes would be compared as part of the value");
    }

    @Test
    void aValueThatIsQueryStringSyntaxStaysOneParameter() {
        assertNull(store.get("a b&c=d,e\"f"));

        assertEquals("select=*&username=eq.a%20b%26c%3Dd%2Ce%22f&limit=1", only().rawQuery,
                "an ampersand or equals sign in a value must not start another parameter");
        assertEquals("select=*&username=eq.a b&c=d,e\"f&limit=1", only().query);
    }

    @Test
    void savingAUserUpsertsOnTheUsername() {
        store.save(new User("p1", "hash", "Pat", Role.PLAYER, false));

        final Recorded call = only();
        assertEquals("POST", call.method);
        assertEquals("/rest/v1/users", call.path);
        assertEquals("on_conflict=username", call.query);
        assertEquals("resolution=merge-duplicates,return=minimal", call.prefer);

        final JSONObject sent = new JSONObject(call.body);
        assertEquals("p1", sent.getString("username"));
        assertEquals("hash", sent.getString("password"));
        assertFalse(sent.getBoolean("active"));
    }

    @Test
    void changingARolePatchesOnlyThatColumnOfThatRow() {
        respond(200, "[{\"username\":\"p1\"}]");

        store.updateRole("p1", Role.COACH);

        final Recorded call = only();
        assertEquals("PATCH", call.method);
        assertEquals("/rest/v1/users", call.path);
        assertEquals("select=username&username=eq.p1", call.query);
        assertEquals("return=representation", call.prefer);
        final JSONObject sent = new JSONObject(call.body);
        assertEquals(1, sent.length(), "no credential or name may travel with a role change");
        assertEquals("COACH", sent.getString("role"));
    }

    @Test
    void changingTheStatusPatchesOnlyTheActiveColumn() {
        respond(200, "[{\"username\":\"p1\"}]");

        store.updateActive("p1", false);

        final JSONObject sent = new JSONObject(only().body);
        assertEquals(1, sent.length());
        assertFalse(sent.getBoolean("active"));
    }

    @Test
    void aPatchThatMatchedNoRowIsReportedBecausePostgrestCallsItASuccess() {
        respond(200, "[]");

        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> store.updateRole("ghost", Role.COACH));

        assertTrue(thrown.getMessage().contains("No account found for ghost"),
                thrown.getMessage());
    }

    @Test
    void anActiveUserIsSentWithTheFlagSpelledOut() {
        store.save(new User("p2", "hash", "Pat", Role.PLAYER));

        assertTrue(new JSONObject(only().body).getBoolean("active"),
                "the mapper omits a true flag, so the store must add it back");
    }

    @Test
    void everyAccountIsReadFromTheTable() {
        respond(200, "[" + USER_ROW + ",{\"username\":\"p1\",\"password\":\"hash\","
                + "\"displayName\":\"Pat\",\"role\":\"PLAYER\"}]");

        assertEquals(2, store.getAllUsers().size());
        assertEquals(1, recorded.size(), "getAllUsers reads the table in one request");
    }

    // ------------------------------------------------------------------
    // Teams, events and attendance
    // ------------------------------------------------------------------

    private static final String EVENT_ROW = "{\"eventId\":\"e1\",\"teamId\":\"t1\",\"type\":\"GAME\","
            + "\"dateTime\":\"2026-11-30T18:00\",\"location\":\"Gym A\",\"opponent\":\"Bears\",\"cancelled\":false}";
    private static final String TEAM_ROW = "{\"teamId\":\"t1\",\"name\":\"Falcons\",\"sport\":\"Soccer\","
            + "\"coachUsername\":\"coach1\",\"playerUsernames\":[]}";
    private static final String NOTICE_ROW = "{\"noticeId\":\"n1\",\"parentUsername\":\"parent1\","
            + "\"playerUsername\":\"player1\",\"eventId\":\"e1\",\"reason\":\"Appointment\","
            + "\"submittedAt\":\"2026-11-23T09:00\"}";

    private static Event game() {
        return new Event("e1", "t1", Event.EventType.GAME,
                LocalDateTime.of(2026, 11, 30, 18, 0), "Gym A", "Bears");
    }

    private static entity.AbsenceNotice notice(String id, String parent, String player, String eventId) {
        return new entity.AbsenceNotice(id, parent, player, eventId, "Appointment",
                LocalDateTime.of(2026, 11, 23, 9, 0));
    }

    @Test
    void creatingAnAccountInsertsInsteadOfUpserting() {
        store.createUser(new User("p1", "hash", "Pat", Role.PLAYER));

        final Recorded call = only();
        assertEquals("POST", call.method);
        assertEquals("/rest/v1/users", call.path);
        assertNull(call.query, "a strict insert must not ask for merge-duplicates");
        assertEquals("return=minimal", call.prefer);
        assertEquals("p1", new JSONObject(call.body).getString("username"));
        assertTrue(new JSONObject(call.body).getBoolean("active"));
    }

    @Test
    void aDuplicateAccountIsReportedInsteadOfReplacingTheStoredOne() {
        respond(409, "{\"code\":\"23505\",\"message\":\"duplicate key value\"}");

        final DuplicateUsernameException thrown = assertThrows(DuplicateUsernameException.class,
                () -> store.createUser(new User("p1", "hash", "Pat", Role.PLAYER)));

        assertEquals("p1", thrown.getUsername());
    }

    @Test
    void aNewEventIsUpsertedOnTheEventIdAndRecordedForTheNotificationFeed() {
        routeBodies.put("/rest/v1/teams", "[" + TEAM_ROW + "]");

        store.saveEvent(game());

        assertEquals(4, recorded.size(), "read the old event, read the team, write the event, write the note");
        final Recorded write = recorded.get(2);
        assertEquals("POST", write.method);
        assertEquals("/rest/v1/events", write.path);
        assertEquals("on_conflict=eventId", write.query);
        assertEquals("GAME", new JSONObject(write.body).getString("type"));

        final Recorded note = recorded.get(3);
        assertEquals("POST", note.method);
        assertEquals("/rest/v1/event_notifications", note.path);
        assertNull(note.query, "a strict insert: a repeated id is an error, not a replacement");
        final JSONObject body = new JSONObject(note.body);
        assertEquals("CREATED", body.getString("changeType"));
        assertEquals("e1", body.getString("eventId"));
        assertTrue(body.getString("message").startsWith("Falcons game scheduled"), body.getString("message"));
    }

    @Test
    void savingAnEventWithoutChangingItRecordsNothing() {
        routeBodies.put("/rest/v1/events", "[" + EVENT_ROW + "]");
        routeBodies.put("/rest/v1/teams", "[" + TEAM_ROW + "]");

        store.saveEvent(game());

        assertEquals(3, recorded.size());
        assertTrue(recorded.stream().noneMatch(call -> call.path.endsWith("event_notifications")));
    }

    @Test
    void aCancelledEventIsRecordedAsCancelled() {
        routeBodies.put("/rest/v1/events", "[" + EVENT_ROW + "]");
        routeBodies.put("/rest/v1/teams", "[" + TEAM_ROW + "]");
        final Event cancelled = game();
        cancelled.cancel();

        store.updateEvent(cancelled);

        assertEquals("CANCELLED", new JSONObject(recorded.get(3).body).getString("changeType"));
    }

    @Test
    void anEventThatWasSavedButNotRecordedSaysSo() {
        routeBodies.put("/rest/v1/teams", "[" + TEAM_ROW + "]");
        routeStatuses.put("/rest/v1/event_notifications", 500);

        final IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> store.saveEvent(game()));

        assertTrue(refused.getMessage().startsWith("The event was saved, but its notification"),
                refused.getMessage());
        assertEquals("/rest/v1/events", recorded.get(2).path, "the event itself was written first");
    }

    @Test
    void aFamilyLinkIsUpsertedOnBothKeyColumns() {
        store.saveParentPlayerLink(new entity.ParentPlayerLink("parent1", "player1"));

        final Recorded write = only();
        assertEquals("POST", write.method);
        assertEquals("/rest/v1/parent_links", write.path);
        assertEquals("on_conflict=parentUsername,playerUsername", write.query);
    }

    @Test
    void aParentSeesOnlyTheirOwnLinkedPlayers() {
        routeBodies.put("/rest/v1/parent_links", "[{\"parentUsername\":\"parent1\",\"playerUsername\":\"player1\"},"
                + "{\"parentUsername\":\"parent2\",\"playerUsername\":\"player2\"}]");

        assertEquals(java.util.List.of("player1"), store.getLinkedPlayerUsernames("parent1"));
        assertEquals(java.util.List.of(), store.getLinkedPlayerUsernames("parent3"));
    }

    @Test
    void removingAFamilyLinkDeletesExactlyThatPair() {
        store.deleteParentPlayerLink("parent1", "player1");

        final Recorded call = only();
        assertEquals("DELETE", call.method);
        assertEquals("/rest/v1/parent_links", call.path);
        assertEquals("parentUsername=eq.parent1&playerUsername=eq.player1", call.query);
    }

    @Test
    void anAbsenceNoticeIsRefusedWhenItsIdBelongsToAnotherAbsence() {
        routeBodies.put("/rest/v1/absence_notices", "[" + NOTICE_ROW + "]");

        final IllegalStateException clash = assertThrows(IllegalStateException.class,
                () -> store.saveAbsenceNotice(notice("n1", "parent1", "player2", "e1")));

        assertTrue(clash.getMessage().contains("different absence"), clash.getMessage());
        assertTrue(recorded.stream().noneMatch(call -> call.method.equals("POST")));
    }

    @Test
    void aSecondNoticeForTheSameParentChildAndEventIsRefused() {
        routeBodies.put("/rest/v1/absence_notices", "[" + NOTICE_ROW + "]");

        final IllegalStateException clash = assertThrows(IllegalStateException.class,
                () -> store.saveAbsenceNotice(notice("n2", "parent1", "player1", "e1")));

        assertTrue(clash.getMessage().contains("already exists"), clash.getMessage());
        assertTrue(recorded.stream().noneMatch(call -> call.method.equals("POST")));
    }

    @Test
    void anUpdatedNoticeIsUpsertedOnItsId() {
        routeBodies.put("/rest/v1/absence_notices", "[" + NOTICE_ROW + "]");

        store.saveAbsenceNotice(notice("n1", "parent1", "player1", "e1"));

        final Recorded write = recorded.get(recorded.size() - 1);
        assertEquals("POST", write.method);
        assertEquals("on_conflict=noticeId", write.query);
    }

    @Test
    void notificationsAreReadOldestFirst() {
        routeBodies.put("/rest/v1/event_notifications", "[{\"notificationId\":\"x\",\"eventId\":\"e1\","
                + "\"teamId\":\"t1\",\"changeType\":\"CREATED\",\"message\":\"m\","
                + "\"createdAt\":\"2026-11-23T10:00\"}]");

        assertEquals(1, store.getEventNotifications().size());

        assertEquals("select=*&order=createdAt.asc", only().query);
    }

    @Test
    void whatEveryStoreAnswersIsBuiltFromTheStoredAccountsTeamsAndLinks() {
        routeBodies.put("/rest/v1/users", "[{\"username\":\"parent1\",\"password\":\"x\","
                + "\"displayName\":\"P\",\"role\":\"PARENT\"}]");
        routeBodies.put("/rest/v1/parent_links",
                "[{\"parentUsername\":\"parent1\",\"playerUsername\":\"player1\"}]");
        routeBodies.put("/rest/v1/teams", "[{\"teamId\":\"t1\",\"name\":\"Falcons\",\"sport\":\"Soccer\","
                + "\"coachUsername\":\"coach1\",\"playerUsernames\":[\"player1\"]},"
                + "{\"teamId\":\"t2\",\"name\":\"Bears\",\"sport\":\"Soccer\","
                + "\"coachUsername\":\"coach1\",\"playerUsernames\":[]}]");

        assertEquals(java.util.List.of("t1"), store.getTeamIdsForMember("parent1"),
                "a parent sees only the teams their linked child plays for");
    }

    @Test
    void teamExistenceAndEventReadsFilterOnTheirKey() {
        assertFalse(store.teamExists("t1"));
        assertEquals("select=*&teamId=eq.t1&limit=1", only().query);

        assertNull(store.getEvent("e1"));
        assertEquals("/rest/v1/events", recorded.get(1).path);
        assertEquals("select=*&eventId=eq.e1&limit=1", recorded.get(1).query);
    }

    @Test
    void attendanceUpsertsOnBothKeyColumns() {
        store.saveAttendance(new AttendanceRecord("e1", "p1", AttendanceRecord.Status.PRESENT));

        final String query = only().query.replace("%2C", ",");
        assertTrue(query.contains("eventId,playerUsername"),
                "a composite key needs both columns as the conflict target, got: " + query);
        assertTrue(query.startsWith("on_conflict="));
    }

    // ------------------------------------------------------------------
    // Announcements
    // ------------------------------------------------------------------

    @Test
    void anAnnouncementIsInsertedStrictly() {
        store.saveAnnouncement(announcement());

        final Recorded call = only();
        assertEquals("POST", call.method);
        assertEquals("/rest/v1/announcements", call.path);
        assertNull(call.query, "a strict insert must not ask for merge-duplicates");
        assertEquals("return=minimal", call.prefer);
        assertEquals("post-1", new JSONObject(call.body).getString("announcementId"));
    }

    @Test
    void aDuplicateAnnouncementIsReportedInsteadOfReplacingThePost() {
        respond(409, "{\"code\":\"23505\",\"message\":\"duplicate key value\"}");

        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> store.saveAnnouncement(announcement()));

        assertTrue(thrown.getMessage().contains("already exists with id post-1"));
    }

    @Test
    void announcementsAreReadForOneTeamInOrder() {
        store.getAnnouncementsForTeam("t1");

        assertEquals("select=*&teamId=eq.t1&order=createdAt.asc", only().query);
    }

    @Test
    void theAnnouncementFilterSendsATeamIdTheSameWayAsAnAccountLookup() {
        store.getAnnouncementsForTeam("demo.team");

        assertEquals("select=*&teamId=eq.demo.team&order=createdAt.asc", only().rawQuery,
                "every filtered read goes through PostgrestClient.where, so a dotted id is "
                        + "sent unquoted exactly like a dotted username");
    }

    // ------------------------------------------------------------------
    // Failures and configuration
    // ------------------------------------------------------------------

    @Test
    void aRejectedRequestBecomesAnIllegalStateExceptionWithTheStatus() {
        respond(401, "{\"message\":\"Invalid API key\"}");

        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> store.get("coach1"));

        assertTrue(thrown.getMessage().contains("HTTP 401"));
        assertTrue(thrown.getMessage().contains("Invalid API key"));
    }

    @Test
    void aTwoHundredThatIsNotJsonIsExplainedInsteadOfThrown() {
        respond(200, "<html><body>This project is paused</body></html>");

        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> store.get("coach1"));

        assertTrue(thrown.getMessage().contains("not JSON"), thrown.getMessage());
        assertTrue(thrown.getMessage().contains("This project is paused"),
                "the message shows what came back: " + thrown.getMessage());
    }

    @Test
    void aRowThatCannotBeReadNamesTheRecord() {
        respond(200, "[{\"username\":\"x\",\"password\":\"hash\",\"displayName\":\"X\","
                + "\"role\":\"SUPERADMIN\"}]");

        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> store.getAllUsers());

        assertTrue(thrown.getMessage().contains("holds an account that cannot be read"),
                thrown.getMessage());
        assertTrue(thrown.getMessage().contains("SUPERADMIN"), thrown.getMessage());
    }

    @Test
    void theStoreIsOnlyBuiltWhenBothValuesArePresent() {
        assertNull(SupabaseAppDataAccessObject.fromValues(null, "key"));
        assertNull(SupabaseAppDataAccessObject.fromValues("", "key"));
        assertNull(SupabaseAppDataAccessObject.fromValues("https://x.supabase.co", " "));
        assertNull(SupabaseAppDataAccessObject.fromValues("https://x.supabase.co", null));
        org.junit.jupiter.api.Assertions.assertNotNull(
                SupabaseAppDataAccessObject.fromValues("https://x.supabase.co", "key"));
    }

    @Test
    void theStoreNamesItsProjectSoTheSignInPageCanShowIt() {
        assertTrue(store.describe().startsWith("Hosted database"));
        assertTrue(store.describe().contains(baseUrl()));
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    void aPastedRestUrlIsNormalised() {
        assertEquals("https://x.supabase.co",
                SupabaseAppDataAccessObject.normaliseProjectUrl("https://x.supabase.co/rest/v1/"));
        assertEquals("https://x.supabase.co",
                SupabaseAppDataAccessObject.normaliseProjectUrl("  https://x.supabase.co  "));
    }

    private static Announcement announcement() {
        return new Announcement("post-1", "t1", "coach1", "Bring water",
                LocalDateTime.of(2026, 11, 24, 9, 0));
    }
}
