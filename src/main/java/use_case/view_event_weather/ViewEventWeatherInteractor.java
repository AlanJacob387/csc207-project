package use_case.view_event_weather;

import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;

/** Checks current membership before requesting, and again before returning, event weather. */
public class ViewEventWeatherInteractor implements ViewEventWeatherInputBoundary {
    private final ViewEventWeatherDataAccessInterface data;
    private final ViewEventWeatherOutputBoundary presenter;
    private final EventWeatherGateway gateway;
    private final Clock clock;

    public ViewEventWeatherInteractor(ViewEventWeatherDataAccessInterface data,
                                     ViewEventWeatherOutputBoundary presenter, EventWeatherGateway gateway) {
        this(data, presenter, gateway, Clock.systemDefaultZone());
    }

    public ViewEventWeatherInteractor(ViewEventWeatherDataAccessInterface data,
                                     ViewEventWeatherOutputBoundary presenter,
                                     EventWeatherGateway gateway, Clock clock) {
        this.data = data;
        this.presenter = presenter;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Override
    public void execute(ViewEventWeatherInputData input) {
        if (input == null || input.username() == null || input.username().isBlank()
                || input.eventId() == null || input.eventId().isBlank()) {
            presenter.prepareFailView("Sign in and choose an event to view its weather.");
            return;
        }
        final ViewEventWeatherOutputData output;
        try {
            final Event event = authorizedEvent(input.username(), input.eventId());
            if (event == null) {
                presenter.prepareFailView("This event is unavailable or you cannot view its weather.");
                return;
            }
            final LocalDateTime eventTime = event.getDateTime();
            final String storedLocation = event.getLocation();
            final LocalDate date = eventTime.toLocalDate();
            final String unavailable = unavailableReason(event);
            if (unavailable != null) {
                output = new ViewEventWeatherOutputData(event.getEventId(), date, storedLocation,
                        null, unavailable, false);
            }
            else if (!input.fetchForecast()) {
                output = new ViewEventWeatherOutputData(event.getEventId(), date, storedLocation,
                        null, "Choose Check weather to load a forecast.", true);
            }
            else {
                final String query = input.locationQuery() == null || input.locationQuery().isBlank()
                        ? storedLocation : input.locationQuery().trim();
                final WeatherForecast forecast = gateway.getForecast(query, date);
                if (!date.equals(forecast.date())) {
                    throw new IllegalStateException("The provider returned a forecast for a different day.");
                }
                final Event latest = authorizedEvent(input.username(), input.eventId());
                if (latest == null || latest.isCancelled() || !eventTime.equals(latest.getDateTime())
                        || !Objects.equals(storedLocation, latest.getLocation())) {
                    presenter.prepareFailView("The event or your access changed. Reopen the event before checking weather.");
                    return;
                }
                output = new ViewEventWeatherOutputData(event.getEventId(), date, storedLocation,
                        forecast, forecast.stale() ? "Offline: showing the last usable forecast."
                                : "Forecast loaded.", true);
            }
        }
        catch (IllegalStateException | IllegalArgumentException | UncheckedIOException ex) {
            presenter.prepareFailView("Weather unavailable: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(output);
    }

    private String unavailableReason(Event event) {
        if (event.isCancelled()) {
            return "Weather is unavailable for a cancelled event.";
        }
        final LocalDate today = LocalDate.now(clock);
        final LocalDate date = event.getDateTime().toLocalDate();
        if (date.isBefore(today) || date.isAfter(today.plusDays(15))) {
            return "Forecasts are available from today through the next 15 days. Check closer to the event.";
        }
        return null;
    }

    private Event authorizedEvent(String username, String eventId) {
        final User user = data.get(username);
        final Event event = data.getEvent(eventId);
        if (user == null || event == null || event.getDateTime() == null) {
            return null;
        }
        final Team team = data.getTeam(event.getTeamId());
        if (team == null) {
            return null;
        }
        if (user.getRole() == Role.ADMIN
                || user.getRole() == Role.COACH && username.equals(team.getCoachUsername())
                || user.getRole() == Role.PLAYER && team.getPlayerUsernames().contains(username)) {
            return event;
        }
        if (user.getRole() == Role.PARENT) {
            final List<String> linked = data.getLinkedPlayerUsernames(username);
            if (linked != null && linked.stream().anyMatch(team.getPlayerUsernames()::contains)) {
                return event;
            }
        }
        return null;
    }
}
