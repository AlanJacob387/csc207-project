package data_access;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import use_case.view_event_weather.EventWeatherGateway;
import use_case.view_event_weather.WeatherForecast;

/**
 * Gateway to the Open-Meteo forecast API (https://open-meteo.com/en/docs).
 *
 * No key and no account are needed: a plain GET returns JSON. The forecast is
 * shown next to a scheduled practice or game, and a high chance of rain flags
 * the event as at risk of cancellation.
 *
 * Example request:
 * https://api.open-meteo.com/v1/forecast?latitude=43.66&longitude=-79.39
 *   &daily=temperature_2m_max,precipitation_probability_max&timezone=auto
 *
 * Network trouble surfaces as IllegalStateException so callers can show a
 * friendly message instead of crashing.
 */
public class OpenMeteoWeatherGateway implements EventWeatherGateway {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String DAILY = "daily";
    private static final String TIME = "time";
    private static final String MAX_TEMPERATURE = "temperature_2m_max";
    private static final String PRECIPITATION = "precipitation_probability_max";

    /** Chance of rain, in percent, at which an outdoor event looks risky. */
    private static final int RAIN_RISK_PERCENT = 60;

    private final OkHttpClient client;
    private final String forecastUrl;
    private final String geocodingUrl;
    private final Clock clock;
    private final Map<String, WeatherForecast> cache = new ConcurrentHashMap<>();
    private final Map<String, ResolvedPlace> places = new ConcurrentHashMap<>();
    private static final Duration FRESH_FOR = Duration.ofMinutes(15);
    private static final Duration STALE_FOR = Duration.ofHours(24);

    /** Uses the public endpoints only when a caller explicitly requests weather. */
    public OpenMeteoWeatherGateway() {
        this(new OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).build(),
                BASE_URL, GEOCODING_URL, Clock.systemDefaultZone());
    }

    /** Accepts local test endpoints, a bounded HTTP client, and a reproducible clock. */
    public OpenMeteoWeatherGateway(OkHttpClient client, String forecastUrl,
                                  String geocodingUrl, Clock clock) {
        this.client = client;
        this.forecastUrl = forecastUrl;
        this.geocodingUrl = geocodingUrl;
        this.clock = clock;
    }

    /** The forecast for one day at one place. */
    public static class DailyForecast {
        private final LocalDate date;
        private final double maxTemperatureCelsius;
        private final int precipitationProbabilityPercent;

        /**
         * Creates a forecast.
         *
         * @param date                            the day
         * @param maxTemperatureCelsius           high temperature, in Celsius
         * @param precipitationProbabilityPercent chance of precipitation, in percent
         */
        public DailyForecast(LocalDate date, double maxTemperatureCelsius,
                             int precipitationProbabilityPercent) {
            this.date = date;
            this.maxTemperatureCelsius = maxTemperatureCelsius;
            this.precipitationProbabilityPercent = precipitationProbabilityPercent;
        }

        public LocalDate getDate() {
            return date;
        }

        public double getMaxTemperatureCelsius() {
            return maxTemperatureCelsius;
        }

        public int getPrecipitationProbabilityPercent() {
            return precipitationProbabilityPercent;
        }

        /**
         * Says whether rain is likely enough to threaten an outdoor event.
         *
         * @return true if the chance of precipitation is at least 60 percent
         */
        public boolean isRainRisk() {
            return precipitationProbabilityPercent >= RAIN_RISK_PERCENT
                    && precipitationProbabilityPercent <= 100;
        }
    }

    /**
     * Fetches the forecast for one day.
     *
     * @param latitude  the venue's latitude
     * @param longitude the venue's longitude
     * @param date      the day to look up
     * @return that day's forecast
     * @throws IllegalStateException if the API cannot be reached or has no data for the day
     */
    public DailyForecast getForecast(double latitude, double longitude, LocalDate date) {
        final WeatherForecast forecast = getForecast(latitude + "," + longitude, date);
        return new DailyForecast(forecast.date(), forecast.maxTemperatureCelsius(),
                forecast.precipitationProbabilityPercent());
    }

    /** Looks up a city (optionally qualified by region) or explicit latitude,longitude. */
    @Override
    public WeatherForecast getForecast(String location, LocalDate date) {
        if (location == null || location.isBlank() || date == null) {
            throw new IllegalArgumentException("Enter a city such as Toronto, Ontario or latitude,longitude.");
        }
        final LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today) || date.isAfter(today.plusDays(15))) {
            throw new IllegalStateException("The event is outside the 16-day forecast window.");
        }
        final String normalized = location.trim();
        final String key = normalized.toLowerCase(Locale.ROOT) + "|" + date;
        final WeatherForecast previous = cache.get(key);
        if (usable(previous, FRESH_FOR)) {
            return previous;
        }
        WeatherForecast fallback = previous;
        try {
            final ResolvedPlace place = resolve(normalized);
            final String coordinateKey = place.latitude() + "," + place.longitude() + "|" + date;
            WeatherForecast forecast = cache.get(coordinateKey);
            if (usable(forecast, STALE_FOR)) {
                fallback = forecast;
            }
            if (!usable(forecast, FRESH_FOR)) {
                final HttpUrl url = HttpUrl.get(forecastUrl).newBuilder()
                        .addQueryParameter("latitude", Double.toString(place.latitude()))
                        .addQueryParameter("longitude", Double.toString(place.longitude()))
                        .addQueryParameter(DAILY, MAX_TEMPERATURE + "," + PRECIPITATION)
                        .addQueryParameter("timezone", "auto")
                        .addQueryParameter("forecast_days", "16").build();
                final String json = request(url);
                final DailyForecast parsed = parse(json, date);
                final Instant fetchedAt = clock.instant();
                forecast = new WeatherForecast(date, parsed.getMaxTemperatureCelsius(),
                        parsed.getPrecipitationProbabilityPercent(), place.name(), fetchedAt, false);
                // One response can cover several events at the same venue on different days.
                final JSONArray days = new JSONObject(json).getJSONObject(DAILY).getJSONArray(TIME);
                for (int i = 0; i < days.length(); i++) {
                    try {
                        final LocalDate day = LocalDate.parse(days.getString(i));
                        final DailyForecast available = parse(json, day);
                        cache.put(place.latitude() + "," + place.longitude() + "|" + day,
                                new WeatherForecast(day, available.getMaxTemperatureCelsius(),
                                        available.getPrecipitationProbabilityPercent(), place.name(), fetchedAt, false));
                    }
                    catch (IllegalArgumentException | IllegalStateException | JSONException | DateTimeException ex) {
                        // Missing values on another day do not invalidate the requested day's forecast.
                    }
                }
            }
            cache.put(key, forecast);
            return forecast;
        }
        catch (IllegalStateException | IllegalArgumentException | JSONException ex) {
            if (usable(fallback, STALE_FOR)) {
                return new WeatherForecast(fallback.date(), fallback.maxTemperatureCelsius(),
                        fallback.precipitationProbabilityPercent(), fallback.location(), fallback.fetchedAt(), true);
            }
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private boolean usable(WeatherForecast forecast, Duration maximumAge) {
        return forecast != null && !forecast.fetchedAt().isAfter(clock.instant())
                && Duration.between(forecast.fetchedAt(), clock.instant()).compareTo(maximumAge) <= 0;
    }

    private ResolvedPlace resolve(String query) {
        final ResolvedPlace known = places.get(query.toLowerCase(Locale.ROOT));
        if (known != null) {
            return known;
        }
        final String[] pair = query.split(",", -1);
        if (pair.length == 2) {
            try {
                final double latitude = Double.parseDouble(pair[0].trim());
                final double longitude = Double.parseDouble(pair[1].trim());
                checkCoordinates(latitude, longitude);
                return new ResolvedPlace(latitude, longitude, query);
            }
            catch (NumberFormatException ex) {
                // A city and region, rather than a coordinate pair, uses geocoding below.
            }
        }
        JSONObject result = geocode(query);
        if (result == null && query.contains(",")) {
            // For "Gym A, Toronto, Ontario", retry the city after the venue name.
            result = geocode(query.substring(query.indexOf(',') + 1).trim());
        }
        if (result == null) {
            throw new IllegalStateException("Location not found. Use a city such as Toronto, Ontario or latitude,longitude.");
        }
        final double latitude = result.getDouble("latitude");
        final double longitude = result.getDouble("longitude");
        checkCoordinates(latitude, longitude);
        final String name = result.optString("name", query)
                + (result.has("admin1") ? ", " + result.getString("admin1") : "")
                + (result.has("country") ? ", " + result.getString("country") : "");
        final ResolvedPlace place = new ResolvedPlace(latitude, longitude, name);
        places.put(query.toLowerCase(Locale.ROOT), place);
        return place;
    }

    private JSONObject geocode(String query) {
        final HttpUrl url = HttpUrl.get(geocodingUrl).newBuilder().addQueryParameter("name", query)
                .addQueryParameter("count", "1").addQueryParameter("language", "en")
                .addQueryParameter("format", "json").build();
        final JSONArray results = new JSONObject(request(url)).optJSONArray("results");
        return results == null || results.isEmpty() ? null : results.getJSONObject(0);
    }

    private String request(HttpUrl url) {
        try (Response response = client.newCall(new Request.Builder().url(url).build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IllegalStateException("Open-Meteo returned HTTP " + response.code() + ". Try again later.");
            }
            final ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Open-Meteo returned an empty response.");
            }
            return body.string();
        }
        catch (IOException ex) {
            throw new IllegalStateException("Open-Meteo could not be reached. Check your connection and try again.", ex);
        }
    }

    private static void checkCoordinates(double latitude, double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Latitude must be -90 to 90 and longitude -180 to 180.");
        }
    }

    private record ResolvedPlace(double latitude, double longitude, String name) { }

    /**
     * Builds the request URL. Package-private so it can be unit tested offline.
     *
     * @param latitude  the venue's latitude
     * @param longitude the venue's longitude
     * @return the full request URL
     */
    static String buildUrl(double latitude, double longitude) {
        return BASE_URL + "?latitude=" + latitude + "&longitude=" + longitude
                + "&daily=" + MAX_TEMPERATURE + "," + PRECIPITATION + "&timezone=auto&forecast_days=16";
    }

    /**
     * Reads one day out of an Open-Meteo response.
     * Package-private so it can be unit tested offline.
     *
     * @param json the response body
     * @param date the day to read
     * @return that day's forecast
     */
    static DailyForecast parse(String json, LocalDate date) {
        final JSONObject daily = new JSONObject(json).getJSONObject(DAILY);
        final JSONArray days = daily.getJSONArray(TIME);
        for (int i = 0; i < days.length(); i++) {
            if (date.toString().equals(days.getString(i))) {
                final Object temperatureValue = daily.getJSONArray(MAX_TEMPERATURE).opt(i);
                final Object probabilityValue = daily.getJSONArray(PRECIPITATION).opt(i);
                if (!(temperatureValue instanceof Number temperature)
                        || !(probabilityValue instanceof Number probability)
                        || !Double.isFinite(temperature.doubleValue())
                        || !Double.isFinite(probability.doubleValue())
                        || probability.doubleValue() < 0 || probability.doubleValue() > 100
                        || probability.doubleValue() != probability.intValue()) {
                    throw new IllegalStateException("Open-Meteo has no usable temperature or rain probability for " + date);
                }
                return new DailyForecast(date, temperature.doubleValue(), probability.intValue());
            }
        }
        throw new IllegalStateException("Open-Meteo returned no forecast for " + date);
    }
}
