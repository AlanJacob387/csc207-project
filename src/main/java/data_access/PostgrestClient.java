package data_access;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The wire half of the hosted store: how a table is addressed, how a request is
 * sent, and how a failure is explained.
 *
 * It knows nothing about accounts, teams, events or announcements - only JSON in,
 * JSON out - so {@link SupabaseAppDataAccessObject} can be about records while
 * this class is about HTTP. That split is what keeps one change (a timeout, an
 * error format) from touching five entity types.
 */
final class PostgrestClient {

    /** Path every table hangs off. */
    static final String REST_PATH = "/rest/v1";

    /** Every column, which is what every read asks for. */
    static final String SELECT_ALL = "*";

    /** PostgREST's answer for a duplicate key on a strict insert. */
    private static final int CONFLICT = 409;

    /** How much of an answer a message shows. */
    private static final int BODY_PREVIEW = 240;

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String projectUrl;
    private final String apiKey;
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    /**
     * Creates the client.
     *
     * @param projectUrl the bare project URL, with no REST suffix
     * @param apiKey     the publishable key
     */
    PostgrestClient(String projectUrl, String apiKey) {
        this.projectUrl = projectUrl;
        this.apiKey = apiKey;
    }

    /** Reads every row of a table. */
    JSONArray readAll(String table) {
        final HttpUrl url = tableUrl(table).addQueryParameter("select", SELECT_ALL).build();
        return parseArray(execute(request(url).get().build(), "the read from " + table));
    }

    /**
     * Reads the rows where one column has a value.
     *
     * @param limit the maximum number of rows, or 0 for no limit
     */
    JSONArray selectWhere(String table, String column, String value, int limit) {
        HttpUrl.Builder url = where(tableUrl(table).addQueryParameter("select", SELECT_ALL),
                column, value);
        if (limit > 0) {
            url = url.addQueryParameter("limit", Integer.toString(limit));
        }
        return parseArray(execute(request(url.build()).get().build(), "the read from " + table));
    }

    /**
     * Adds an equality filter to a route, so every filtered read goes through one
     * place and no caller has to remember how a value has to be written.
     *
     * The value is percent-encoded as one query parameter and otherwise sent as it
     * is, the way the Supabase client libraries send an {@code eq} filter. It is
     * not wrapped in quotes: PostgREST's reserved characters (comma, dot, colon,
     * asterisk, parentheses) only matter inside the list forms ({@code in},
     * {@code or}, {@code and}), which this client never builds, so a name such as
     * {@code pat.smith} is compared as {@code pat.smith}.
     *
     * @param url    the route being built
     * @param column the column to compare
     * @param value  the value to compare against
     * @return the same builder, with the filter added
     */
    HttpUrl.Builder where(HttpUrl.Builder url, String column, String value) {
        return url.addQueryParameter(column, "eq." + value);
    }

    /**
     * Reads whatever a prepared route points at.
     *
     * @param url  the built route
     * @param what what the read is, for the message
     */
    JSONArray get(HttpUrl url, String what) {
        return parseArray(execute(request(url).get().build(), what));
    }

    /** Builds the route for one table. */
    HttpUrl.Builder tableUrl(String table) {
        return HttpUrl.get(projectUrl + REST_PATH + "/" + table).newBuilder();
    }

    /** Inserts a row, or replaces the row with the same conflict columns. */
    void upsert(String table, String conflictColumns, JSONObject row) {
        final HttpUrl url = tableUrl(table)
                .addQueryParameter("on_conflict", conflictColumns)
                .build();
        execute(request(url)
                .post(RequestBody.create(row.toString(), JSON))
                .header("Prefer", "resolution=merge-duplicates,return=minimal")
                .build(), "the write to " + table);
    }

    /**
     * Changes some columns of the rows where one column has a value, and says how
     * many rows that was.
     *
     * Only the named columns are sent, so everything else in the row is left as
     * the database holds it. The answer asks for the key column back so a PATCH
     * that matched nothing - which PostgREST reports as a success - can be told
     * apart from one that changed a row.
     *
     * @param table   the table to change
     * @param column  the column to match
     * @param value   the value to match
     * @param columns the new values
     * @param what    what the write is, for a message
     * @return the number of rows that matched
     */
    int update(String table, String column, String value, JSONObject columns, String what) {
        final HttpUrl url = where(tableUrl(table).addQueryParameter("select", column),
                column, value).build();
        return parseArray(execute(request(url)
                .patch(RequestBody.create(columns.toString(), JSON))
                .header("Prefer", "return=representation")
                .build(), what)).length();
    }

    /**
     * Deletes the rows where every named column has its value.
     *
     * @param table  the table to delete from
     * @param equals the columns to match and the value each must have
     * @param what   what the write is, for a message
     */
    void delete(String table, java.util.Map<String, String> equals, String what) {
        HttpUrl.Builder url = tableUrl(table);
        for (java.util.Map.Entry<String, String> match : equals.entrySet()) {
            url = where(url, match.getKey(), match.getValue());
        }
        execute(request(url.build()).delete().header("Prefer", "return=minimal").build(), what);
    }

    /**
     * Inserts a row, letting the duplicate-key answer through.
     *
     * A plain POST uses PostgREST's strict resolution, so a row that already
     * exists answers 409 instead of replacing it. The caller decides what that
     * means for its entity, which is why this throws {@link Conflict} rather than
     * an entity-specific failure.
     *
     * @param table the table to write
     * @param row   the row to insert
     * @param what  what the write is, for a message
     * @throws Conflict when the database answers 409
     */
    void insert(String table, JSONObject row, String what) {
        final HttpUrl url = tableUrl(table).build();
        try (Response response = client.newCall(request(url)
                .post(RequestBody.create(row.toString(), JSON))
                .header("Prefer", "return=minimal")
                .build()).execute()) {
            final String text = bodyOf(response);
            if (response.code() == CONFLICT) {
                throw new Conflict(table);
            }
            if (!response.isSuccessful()) {
                throw new IllegalStateException(describe(response, text, what));
            }
        }
        catch (IOException ex) {
            throw new UncheckedIOException("Could not reach the database: " + ex.getMessage(), ex);
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
    static <T> T mapRow(JSONArray rows, int index,
                        Function<JSONObject, T> mapper, String what) {
        try {
            return mapper.apply(rows.getJSONObject(index));
        }
        catch (JSONException | IllegalArgumentException ex) {
            throw new IllegalStateException("The database holds " + what
                    + " that cannot be read: " + ex.getMessage(), ex);
        }
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

    /** Adds the publishable key and the JSON accept header. */
    private Request.Builder request(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header("apikey", apiKey)
                .header("Accept", "application/json");
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

    /** Shortens an answer or a row for a message. */
    private static String preview(String body) {
        String shown = body == null ? "" : body.replaceAll("\\s+", " ").trim();
        if (shown.length() > BODY_PREVIEW) {
            shown = shown.substring(0, BODY_PREVIEW) + "...";
        }
        return shown;
    }

    /** Thrown when a strict insert finds the key already stored. */
    static final class Conflict extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final transient String table;

        /**
         * Creates the failure.
         *
         * @param table the table that already holds the key
         */
        Conflict(String table) {
            super("A row already exists in " + table);
            this.table = table;
        }

        /**
         * The table that refused the insert.
         *
         * @return the table name
         */
        String getTable() {
            return table;
        }
    }
}
