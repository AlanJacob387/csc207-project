package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import use_case.view_event_weather.WeatherForecast;

/** Intercepts every HTTP exchange locally, including geocoding, errors, and offline fallback. */
class OpenMeteoWeatherTransportTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 23);
    private static final String SAMPLE = "{\"daily\":{\"time\":[\"2026-11-23\",\"2026-11-24\"],"
            + "\"temperature_2m_max\":[12.4,11.0],\"precipitation_probability_max\":[60,59]}}";

    @Test
    void explicitCoordinatesSkipGeocodingAndOneResponseServesSeveralDays() {
        final AtomicInteger requests = new AtomicInteger();
        final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            requests.incrementAndGet();
            assertEquals("/forecast", chain.request().url().encodedPath());
            assertEquals("16", chain.request().url().queryParameter("forecast_days"));
            return response(chain.request(), 200, SAMPLE);
        }).build();
        final OpenMeteoWeatherGateway gateway = gateway(client, new MovingClock());
        assertTrue(gateway.getForecast("43.66,-79.39", DAY).isRainRisk());
        assertFalse(gateway.getForecast("43.66,-79.39", DAY.plusDays(1)).isRainRisk());
        assertEquals(1, requests.get());
    }

    @Test
    void venueFallbackResolvesTheCityAndReturnsItsProviderLabel() {
        final AtomicInteger geoRequests = new AtomicInteger();
        final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            if (chain.request().url().encodedPath().equals("/search")) {
                geoRequests.incrementAndGet();
                final String query = chain.request().url().queryParameter("name");
                return response(chain.request(), 200, query.startsWith("Gym") ? "{}"
                        : "{\"results\":[{\"name\":\"Toronto\",\"admin1\":\"Ontario\","
                        + "\"country\":\"Canada\",\"latitude\":43.66,\"longitude\":-79.39}]}");
            }
            return response(chain.request(), 200, SAMPLE);
        }).build();
        final WeatherForecast forecast = gateway(client, new MovingClock())
                .getForecast("Gym A, Toronto, Ontario", DAY);
        assertEquals("Toronto, Ontario, Canada", forecast.location());
        assertEquals(2, geoRequests.get());
    }

    @Test
    void failedRefreshUsesOnlyARecentUsableForecastAndMarksItStale() {
        final AtomicReference<String> mode = new AtomicReference<>("ok");
        final MovingClock clock = new MovingClock();
        final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            if (mode.get().equals("timeout")) {
                throw new SocketTimeoutException("local timeout");
            }
            return response(chain.request(), mode.get().equals("http") ? 503 : 200, SAMPLE);
        }).build();
        final OpenMeteoWeatherGateway gateway = gateway(client, clock);
        final WeatherForecast first = gateway.getForecast("43.66,-79.39", DAY);
        clock.advance(Duration.ofMinutes(16));
        mode.set("timeout");
        final WeatherForecast stale = gateway.getForecast("43.66, -79.39", DAY);
        assertTrue(stale.stale());
        assertEquals(first.fetchedAt(), stale.fetchedAt());
        assertEquals(60, stale.precipitationProbabilityPercent());
        clock.advance(Duration.ofHours(25));
        assertThrows(IllegalStateException.class, () -> gateway.getForecast("43.66,-79.39", DAY.plusDays(1)));
    }

    @Test
    void httpErrorsMalformedBodiesAndMissingLocationsNeverBecomeSafeForecasts() {
        for (String mode : new String[] {
            "http", "malformed", "missing"
        }) {
            final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain ->
                    response(chain.request(), mode.equals("http") ? 429 : 200,
                            mode.equals("malformed") ? "invalid-json" : "{}" )).build();
            assertThrows(IllegalStateException.class, () -> gateway(client, new MovingClock())
                    .getForecast(mode.equals("missing") ? "Unknown City" : "43.66,-79.39", DAY));
        }
    }

    @Test
    void invalidValuesAndOutOfRangeDatesAreUnavailableWithoutAnyTransport() {
        final AtomicInteger requests = new AtomicInteger();
        final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            requests.incrementAndGet();
            throw new IOException("Transport must not be used");
        }).build();
        final OpenMeteoWeatherGateway gateway = gateway(client, new MovingClock());
        assertThrows(IllegalStateException.class, () -> gateway.getForecast("43,-79", DAY.plusDays(16)));
        assertThrows(IllegalStateException.class, () -> gateway.getForecast("43,-79", DAY.minusDays(1)));
        assertThrows(IllegalStateException.class, () -> gateway.getForecast("91,0", DAY));
        assertEquals(0, requests.get());
        for (String value : new String[] {
            "null", "-1", "101", "59.5", "\"60\""
        }) {
            final String json = SAMPLE.replace("[60,59]", "[" + value + ",59]");
            assertThrows(IllegalStateException.class, () -> OpenMeteoWeatherGateway.parse(json, DAY));
        }
        assertFalse(new OpenMeteoWeatherGateway.DailyForecast(DAY, 10, 101).isRainRisk());
    }

    private OpenMeteoWeatherGateway gateway(OkHttpClient client, Clock clock) {
        return new OpenMeteoWeatherGateway(client, "https://local.test/forecast", "https://local.test/search", clock);
    }

    private static Response response(Request request, int code, String json) {
        return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("Local fixture")
                .body(ResponseBody.create(json, MediaType.get("application/json"))).build();
    }

    private static final class MovingClock extends Clock {
        private Instant now = Instant.parse("2026-11-23T12:00:00Z");
        void advance(Duration duration) {
            now = now.plus(duration);
        }
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }
        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
        @Override
        public Instant instant() {
            return now;
        }
    }
}
