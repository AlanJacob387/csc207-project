package data_access;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Live proof that the Open-Meteo API can be called from our Java program.
 *
 * Tagged "network", so it is skipped by a normal {@code gradlew test} run and
 * only runs when asked: {@code gradlew test -Pnetwork}.
 *
 * Evidence recorded 2026-10-29: this test passed and returned a real forecast
 * for Toronto (43.66, -79.39). See the API section of the README.
 */
@Tag("network")
class OpenMeteoWeatherGatewayLiveTest {

    @Test
    void fetchesARealForecastForToronto() {
        final OpenMeteoWeatherGateway gateway = new OpenMeteoWeatherGateway();

        final OpenMeteoWeatherGateway.DailyForecast forecast =
                gateway.getForecast(43.66, -79.39, LocalDate.now());

        assertNotNull(forecast);
        assertFalse(Double.isNaN(forecast.getMaxTemperatureCelsius()));
        assertTrue(forecast.getPrecipitationProbabilityPercent() >= 0);
        System.out.println("Open-Meteo replied for " + forecast.getDate()
                + ": max " + forecast.getMaxTemperatureCelsius() + " C, rain chance "
                + forecast.getPrecipitationProbabilityPercent() + "%");
    }
}
