package data_access;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;

import entity.Role;
import entity.User;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import use_case.login.LoginUserDataAccessInterface;
import use_case.signup.SignupUserDataAccessInterface;

/**
 * Gateway to the course user API (http://vm003.teach.cs.toronto.edu:20112).
 *
 * The API stores credentials only (username + password) and is per-user, so
 * this DAO owns credentials and delegates the rest of a profile (display name,
 * role) to the local store given in the constructor.
 *
 * Endpoints: GET /user?username=, GET /checkIfUserExists?username=,
 * POST /user (create account), PUT /user (change password).
 *
 * Network trouble and server rejections surface as IllegalStateException, so
 * the login and signup interactors can turn them into a fail view.
 */
public class DBDataAccessObject
        implements LoginUserDataAccessInterface, SignupUserDataAccessInterface {

    private static final String BASE_URL = "http://vm003.teach.cs.toronto.edu:20112";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String STATUS_CODE = "status_code";
    private static final String USER = "user";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String MESSAGE = "message";
    private static final int SUCCESS_CODE = 200;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private final FileAppDataAccessObject profileStore;

    /**
     * Creates the gateway.
     *
     * @param profileStore where display names and roles are kept, because the
     *                     API has nowhere to put them
     */
    public DBDataAccessObject(FileAppDataAccessObject profileStore) {
        this.profileStore = profileStore;
    }

    @Override
    public User get(String username) {
        final JSONObject responseBody = getJson("/user?username=" + encode(username));
        if (responseBody.optInt(STATUS_CODE, -1) != SUCCESS_CODE) {
            return null; // no such user on the server
        }
        final String password = responseBody.getJSONObject(USER).getString(PASSWORD);
        final User profile = profileStore.get(username);
        if (profile == null) {
            // credentials exist, but no profile has been saved yet
            return new User(username, password, username, Role.PLAYER);
        }
        return new User(username, password, profile.getDisplayName(), profile.getRole());
    }

    @Override
    public boolean existsByName(String username) {
        final JSONObject responseBody = getJson("/checkIfUserExists?username=" + encode(username));
        return responseBody.optInt(STATUS_CODE, -1) == SUCCESS_CODE;
    }

    @Override
    public void save(User user) {
        final JSONObject body = new JSONObject();
        body.put(USERNAME, user.getUsername());
        body.put(PASSWORD, user.getPassword());
        requireSuccess(send("POST", "/user", body), "Could not create the account");
        profileStore.saveProfile(user.getUsername(), user.getDisplayName(), user.getRole());
    }

    // ------------------------------------------------------------------
    // HTTP helpers
    // ------------------------------------------------------------------

    /** Sends a GET request and parses the JSON body. */
    private JSONObject getJson(String path) {
        final Request request = new Request.Builder()
                .url(BASE_URL + path)
                .addHeader("Content-Type", CONTENT_TYPE_JSON)
                .build();
        return execute(request);
    }

    /** Sends a POST or PUT request with a JSON body and parses the response. */
    private JSONObject send(String method, String path, JSONObject body) {
        final MediaType mediaType = MediaType.parse(CONTENT_TYPE_JSON);
        final Request request = new Request.Builder()
                .url(BASE_URL + path)
                .method(method, RequestBody.create(body.toString(), mediaType))
                .addHeader("Content-Type", CONTENT_TYPE_JSON)
                .build();
        return execute(request);
    }

    /** Runs a request and returns the parsed body, failing loudly on trouble. */
    private JSONObject execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            final ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Empty response from the user API");
            }
            return new JSONObject(body.string());
        }
        catch (IOException | JSONException ex) {
            throw new IllegalStateException(
                    "Could not reach the user API: " + ex.getMessage(), ex);
        }
    }

    /** Throws if the API did not answer with a 200 status code. */
    private static void requireSuccess(JSONObject response, String context) {
        if (response.optInt(STATUS_CODE, -1) != SUCCESS_CODE) {
            final String message = response.optString(MESSAGE, "unknown error");
            throw new IllegalStateException(context + ": " + message);
        }
    }

    /** URL-encodes a username so it is safe inside a query string. */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
