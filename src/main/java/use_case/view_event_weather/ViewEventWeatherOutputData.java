package use_case.view_event_weather;

import java.time.LocalDate;

/** An authorized event's weather state, including an explicit unavailable or ready message. */
public record ViewEventWeatherOutputData(String eventId, LocalDate date, String location,
                                        WeatherForecast forecast, String message, boolean canCheck) { }
