package data_access;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Builds the legacy Supabase key shape - a three-part JWT - for tests that need a
 * {@code service_role} or {@code anon} key without a real project.
 *
 * The signature is never checked by anything in this app, so a placeholder is
 * enough; what matters is that the payload carries the role, which is what
 * {@link DatabaseConfig#isSecretKey(String)} reads.
 */
public final class LegacyKeys {

    private LegacyKeys() {
    }

    /**
     * Builds a legacy key whose payload names the given role.
     *
     * @param role the role the key would run as, such as {@code service_role}
     * @return a three-part base64url key, the shape the dashboard shows
     */
    public static String of(String role) {
        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final String header = encoder.encodeToString(
                "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        final String payload = encoder.encodeToString(
                ("{\"role\":\"" + role + "\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".c2lnbmF0dXJl";
    }
}