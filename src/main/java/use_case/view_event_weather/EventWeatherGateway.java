package use_case.view_event_weather;

import java.time.LocalDate;

/** Obtains a forecast without exposing a provider's response objects to the use case. */
public interface EventWeatherGateway {
    WeatherForecast getForecast(String location, LocalDate date);
}
