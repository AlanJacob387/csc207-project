package data_access;

import java.io.IOException;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

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
public class OpenMeteoWeatherGateway {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String DAILY = "daily";
    private static final String TIME = "time";
    private static final String MAX_TEMPERATURE = "temperature_2m_max";
    private static final String PRECIPITATION = "precipitation_probability_max";

    /** Chance of rain, in percent, at which an outdoor event looks risky. */
    private static final int RAIN_RISK_PERCENT = 60;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

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
            return precipitationProbabilityPercent >= RAIN_RISK_PERCENT;
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
        final Request request = new Request.Builder()
                .url(buildUrl(latitude, longitude))
                .build();
        try (Response response = client.newCall(request).execute()) {
            final ResponseBody body = response.body();
            if (body == null) {
                throw new IllegalStateException("Empty response from Open-Meteo");
            }
            return parse(body.string(), date);
        }
        catch (IOException | JSONException ex) {
            throw new IllegalStateException(
                    "Could not reach Open-Meteo: " + ex.getMessage(), ex);
        }
    }

    /**
     * Builds the request URL. Package-private so it can be unit tested offline.
     *
     * @param latitude  the venue's latitude
     * @param longitude the venue's longitude
     * @return the full request URL
     */
    static String buildUrl(double latitude, double longitude) {
        return BASE_URL + "?latitude=" + latitude + "&longitude=" + longitude
                + "&daily=" + MAX_TEMPERATURE + "," + PRECIPITATION + "&timezone=auto";
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
                return new DailyForecast(
                        date,
                        daily.getJSONArray(MAX_TEMPERATURE).optDouble(i, Double.NaN),
                        daily.getJSONArray(PRECIPITATION).optInt(i, -1));
            }
        }
        throw new IllegalStateException("Open-Meteo returned no forecast for " + date);
    }
}
