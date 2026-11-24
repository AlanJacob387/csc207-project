package use_case.view_event_weather;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/** A usable daily forecast; missing or invalid provider values never become a safe-weather flag. */
public record WeatherForecast(LocalDate date, double maxTemperatureCelsius,
                              int precipitationProbabilityPercent, String location,
                              Instant fetchedAt, boolean stale) {
    public WeatherForecast {
        Objects.requireNonNull(date);
        Objects.requireNonNull(location);
        Objects.requireNonNull(fetchedAt);
        if (!Double.isFinite(maxTemperatureCelsius)
                || precipitationProbabilityPercent < 0 || precipitationProbabilityPercent > 100) {
            throw new IllegalArgumentException("The forecast has unavailable temperature or precipitation data.");
        }
    }

    public boolean isRainRisk() {
        return precipitationProbabilityPercent >= 60;
    }
}
