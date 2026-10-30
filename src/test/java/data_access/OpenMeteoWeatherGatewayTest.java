package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * Offline tests for the Open-Meteo gateway: URL building and response parsing.
 * The live network call lives in {@link OpenMeteoWeatherGatewayLiveTest}.
 */
class OpenMeteoWeatherGatewayTest {

    /** A trimmed-down copy of a real Open-Meteo response. */
    private static final String SAMPLE = """
            {"latitude":43.7,"longitude":-79.4,"timezone":"America/Toronto",
             "daily":{"time":["2026-10-29","2026-10-30"],
                      "temperature_2m_max":[12.4,9.8],
                      "precipitation_probability_max":[75,10]}}
            """;

    @Test
    void urlAsksForCoordinatesAndDailyFields() {
        final String url = OpenMeteoWeatherGateway.buildUrl(43.66, -79.39);

        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"));
        assertTrue(url.contains("latitude=43.66"));
        assertTrue(url.contains("longitude=-79.39"));
        assertTrue(url.contains("temperature_2m_max"));
        assertTrue(url.contains("precipitation_probability_max"));
        assertTrue(url.contains("timezone=auto"));
    }

    @Test
    void parsesTheRequestedDay() {
        final OpenMeteoWeatherGateway.DailyForecast forecast =
                OpenMeteoWeatherGateway.parse(SAMPLE, LocalDate.of(2026, 10, 29));

        assertEquals(LocalDate.of(2026, 10, 29), forecast.getDate());
        assertEquals(12.4, forecast.getMaxTemperatureCelsius(), 0.001);
        assertEquals(75, forecast.getPrecipitationProbabilityPercent());
    }

    @Test
    void parsesASecondDayFromTheSameResponse() {
        final OpenMeteoWeatherGateway.DailyForecast forecast =
                OpenMeteoWeatherGateway.parse(SAMPLE, LocalDate.of(2026, 10, 30));

        assertEquals(9.8, forecast.getMaxTemperatureCelsius(), 0.001);
        assertEquals(10, forecast.getPrecipitationProbabilityPercent());
    }

    @Test
    void highChanceOfRainIsFlagged() {
        final OpenMeteoWeatherGateway.DailyForecast wet =
                OpenMeteoWeatherGateway.parse(SAMPLE, LocalDate.of(2026, 10, 29));
        final OpenMeteoWeatherGateway.DailyForecast dry =
                OpenMeteoWeatherGateway.parse(SAMPLE, LocalDate.of(2026, 10, 30));

        assertTrue(wet.isRainRisk());
        assertFalse(dry.isRainRisk());
    }

    @Test
    void aDayMissingFromTheResponseThrows() {
        assertThrows(IllegalStateException.class, () ->
                OpenMeteoWeatherGateway.parse(SAMPLE, LocalDate.of(2027, 1, 1)));
    }
}
