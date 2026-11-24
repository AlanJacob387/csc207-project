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

        final byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(responseStatus, bytes.length == 0 ? -1 : bytes.length);
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
    void aUsernameWithAFilterCharacterIsQuotedOnTheWire() {
        assertNull(store.get("pat.smith"));

        assertEquals("select=*&username=eq.%22pat.smith%22&limit=1", only().rawQuery,
                "a dot is PostgREST syntax, so the value has to arrive quoted as %22...%22 "
                        + "to be compared literally");
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
    void anEventIsUpsertedOnTheEventId() {
        store.saveEvent(new Event("e1", "t1", Event.EventType.GAME,
                LocalDateTime.of(2026, 11, 30, 18, 0), "Gym A", "Bears"));

        final Recorded call = only();
        assertEquals("POST", call.method);
        assertEquals("/rest/v1/events", call.path);
        assertEquals("on_conflict=eventId", call.query);
        assertEquals("GAME", new JSONObject(call.body).getString("type"));
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
