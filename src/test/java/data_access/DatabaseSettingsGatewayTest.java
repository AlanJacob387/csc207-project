package data_access;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Tests the gateway's half of the Database Settings contract: it refuses what the
 * rules refuse, and it reports every refusal as the failure the port promises
 * rather than as the argument error {@link DatabaseConfig} raises for its own
 * callers.
 *
 * Nothing here reaches the network: each case is refused before a request is made.
 */
class DatabaseSettingsGatewayTest {

    private static final String URL = "https://demo.supabase.co";

    private final DatabaseSettingsGateway gateway = new DatabaseSettingsGateway();

    @Test
    void aSecretKeyIsReportedAsAFailureRatherThanAnArgumentError() {
        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> gateway.countAccounts(URL, "sb_secret_abc123"));

        assertTrue(thrown.getMessage().contains("secret key"), thrown.getMessage());
    }

    @Test
    void aLegacyServiceRoleKeyIsRefusedBeforeAnyRequest() {
        final String key = LegacyKeys.of("service_role");

        assertThrows(IllegalStateException.class, () -> gateway.countAccounts(URL, key));
        assertThrows(IllegalStateException.class, () -> gateway.save(URL, key));
    }

    @Test
    void aUrlThatCannotCarryTheKeyIsRefused() {
        assertThrows(IllegalStateException.class,
                () -> gateway.countAccounts("http://example.supabase.co", "sb_publishable_key"));
    }

    @Test
    void incompleteSettingsAreRefused() {
        assertThrows(IllegalStateException.class, () -> gateway.countAccounts(URL, "  "));
    }

    @Test
    void aConfiguredUrlThatIsRefusedIsReportedAsAFailureWhenTheCurrentUrlIsRead() {
        Assumptions.assumeTrue(System.getenv(DatabaseConfig.URL_VARIABLE) == null,
                "the environment would be read first");
        final String url = System.getProperty(DatabaseConfig.URL_PROPERTY);
        final String key = System.getProperty(DatabaseConfig.KEY_PROPERTY);
        System.setProperty(DatabaseConfig.URL_PROPERTY, "http://example.supabase.co");
        System.setProperty(DatabaseConfig.KEY_PROPERTY, "sb_publishable_key");
        try {
            final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    gateway::currentUrl);

            assertTrue(thrown.getMessage().contains("https://"), thrown.getMessage());
        }
        finally {
            restore(DatabaseConfig.URL_PROPERTY, url);
            restore(DatabaseConfig.KEY_PROPERTY, key);
        }
    }

    private static void restore(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        }
        else {
            System.setProperty(name, value);
        }
    }

    @Test
    void theDecisionsThePageAsksForAreTheOnesTheConfigApplies() {
        assertTrue(gateway.isSecretKey("sb_secret_abc123"));
        assertTrue(gateway.isSecureUrl("https://demo.supabase.co"));
        assertTrue(gateway.isSecureUrl("http://127.0.0.1:54321"));
    }
}