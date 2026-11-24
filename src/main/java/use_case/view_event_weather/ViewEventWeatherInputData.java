package use_case.view_event_weather;

/** A metadata-only request does not contact a weather service. */
public record ViewEventWeatherInputData(String username, String eventId,
                                       String locationQuery, boolean fetchForecast) { }
