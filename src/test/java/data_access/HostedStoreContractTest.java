package data_access;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Runs the store contract against the hosted store, behind a stub that behaves
 * like the one table the contract uses: a POST without an on_conflict parameter
 * inserts and answers 409 for a username it already holds, a POST with one
 * replaces, and a GET filters on {@code username=eq.…} or returns every row.
 *
 * That is enough of PostgREST for the contract to mean something, and it keeps
 * the run offline: no account, no network, no bill.
 */
class HostedStoreContractTest extends AppDataStoreContractTest {

    private StubUsersTable stub;

    @BeforeEach
    void startStub() throws IOException {
        stub = new StubUsersTable();
    }

    @AfterEach
    void stopStub() {
        stub.close();
    }

    @Override
    protected AppDataStore newStore() {
        return new SupabaseAppDataAccessObject(stub.url(), "sb_publishable_test");
    }

    /** The smallest PostgREST a store contract needs: one users table. */
    private static final class StubUsersTable implements AutoCloseable {

        private final HttpServer server;
        private final Map<String, JSONObject> rows = new LinkedHashMap<>();

        StubUsersTable() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
        }

        String url() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private void handle(HttpExchange exchange) throws IOException {
            final String query = exchange.getRequestURI().getQuery() == null
                    ? "" : exchange.getRequestURI().getQuery();
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, select(query).toString());
                return;
            }
            if ("PATCH".equals(exchange.getRequestMethod())) {
                final JSONObject patch = new JSONObject(new String(
                        exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                final JSONArray changed = new JSONArray();
                for (JSONObject match : matching(query)) {
                    for (String key : patch.keySet()) {
                        match.put(key, patch.get(key));
                    }
                    changed.put(new JSONObject().put("username", match.getString("username")));
                }
                respond(exchange, 200, changed.toString());
                return;
            }
            final JSONObject row = new JSONObject(new String(
                    exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            final String username = row.getString("username");
            if (query.contains("on_conflict=username")) {
                rows.put(username, row);
                respond(exchange, 201, "");
            }
            else if (rows.containsKey(username)) {
                respond(exchange, 409, "{\"code\":\"23505\",\"message\":\"duplicate key\"}");
            }
            else {
                rows.put(username, row);
                respond(exchange, 201, "");
            }
        }

        /** The stored rows the username filter selects, or every row without one. */
        private java.util.List<JSONObject> matching(String query) {
            final java.util.List<JSONObject> matches = new java.util.ArrayList<>();
            final JSONArray selected = select(query);
            for (int i = 0; i < selected.length(); i++) {
                matches.add(selected.getJSONObject(i));
            }
            return matches;
        }

        /** Applies the username filter the store asked for, if any. */
        private JSONArray select(String query) {
            final String marker = "username=eq.";
            final JSONArray selected = new JSONArray();
            String wanted = null;
            if (query.contains(marker)) {
                wanted = query.substring(query.indexOf(marker) + marker.length());
                final int end = wanted.indexOf('&');
                if (end >= 0) {
                    wanted = wanted.substring(0, end);
                }
            }
            for (JSONObject row : rows.values()) {
                if (wanted == null || wanted.equals(row.getString("username"))) {
                    selected.put(row);
                }
            }
            return selected;
        }

        private static void respond(HttpExchange exchange, int status, String body)
                throws IOException {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(bytes);
                }
            }
            exchange.close();
        }
    }
}
