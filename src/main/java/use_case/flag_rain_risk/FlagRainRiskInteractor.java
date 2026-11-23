package use_case.flag_rain_risk;

import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.view_event_weather.EventWeatherGateway;
import use_case.view_event_weather.WeatherForecast;

/** Computes measured rain risk for upcoming events the current reader is allowed to see. */
public class FlagRainRiskInteractor implements FlagRainRiskInputBoundary {
    private final FlagRainRiskDataAccessInterface data;
    private final FlagRainRiskOutputBoundary presenter;
    private final EventWeatherGateway gateway;
    private final Clock clock;

    public FlagRainRiskInteractor(FlagRainRiskDataAccessInterface data,
                                 FlagRainRiskOutputBoundary presenter, EventWeatherGateway gateway) {
        this(data, presenter, gateway, Clock.systemDefaultZone());
    }

    public FlagRainRiskInteractor(FlagRainRiskDataAccessInterface data,
                                 FlagRainRiskOutputBoundary presenter, EventWeatherGateway gateway, Clock clock) {
        this.data = data;
        this.presenter = presenter;
        this.gateway = gateway;
        this.clock = clock;
    }

    @Override
    public void execute(FlagRainRiskInputData input) {
        if (input == null || input.username() == null || input.username().isBlank()) {
            presenter.prepareFailView("Sign in to check upcoming rain risk.");
            return;
        }
        final List<FlagRainRiskOutputData.EventRisk> risks = new ArrayList<>();
        try {
            final User user = data.get(input.username());
            if (user == null) {
                presenter.prepareFailView("Sign in to check upcoming rain risk.");
                return;
            }
            if (input.teamId() != null && !input.teamId().isBlank()
                    && !canRead(user, data.getTeam(input.teamId()))) {
                presenter.prepareFailView("You cannot view the selected team's rain risk.");
                return;
            }
            final List<Event> events = data.getAllEvents().stream().filter(Objects::nonNull)
                    .filter(event -> event.getDateTime() != null && !event.isCancelled())
                    .filter(event -> !event.getDateTime().isBefore(LocalDateTime.now(clock)))
                    .filter(event -> input.teamId() == null || input.teamId().isBlank()
                            || input.teamId().equals(event.getTeamId()))
                    .filter(event -> canRead(user, data.getTeam(event.getTeamId())))
                    .sorted(Comparator.comparing(Event::getDateTime).thenComparing(Event::getEventId)).toList();
            for (Event event : events) {
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                final LocalDateTime eventTime = event.getDateTime();
                final String location = event.getLocation();
                final LocalDate date = eventTime.toLocalDate();
                String status;
                Integer probability = null;
                boolean rainRisk = false;
                boolean stale = false;
                if (date.isAfter(LocalDate.now(clock).plusDays(15))) {
                    status = "Unavailable: outside the 16-day forecast window";
                }
                else {
                    try {
                        final WeatherForecast forecast = gateway.getForecast(location, date);
                        if (!date.equals(forecast.date())) {
                            throw new IllegalStateException("Forecast date did not match the event");
                        }
                        probability = forecast.precipitationProbabilityPercent();
                        rainRisk = forecast.isRainRisk();
                        stale = forecast.stale();
                        status = (rainRisk ? "Rain risk: " : "Below rain-risk threshold: ")
                                + probability + "%" + (stale ? " (stale cached forecast)" : "");
                    }
                    catch (IllegalStateException | IllegalArgumentException | UncheckedIOException ex) {
                        status = "Unavailable: " + ex.getMessage();
                    }
                }
                final User latestUser = data.get(input.username());
                final Event latest = data.getEvent(event.getEventId());
                if (latestUser == null) {
                    presenter.prepareFailView("Your account is no longer available.");
                    return;
                }
                if (latest == null || latest.isCancelled() || !eventTime.equals(latest.getDateTime())
                        || !Objects.equals(location, latest.getLocation())
                        || !canRead(latestUser, data.getTeam(latest.getTeamId()))) {
                    continue;
                }
                risks.add(new FlagRainRiskOutputData.EventRisk(event.getEventId(), event.getTeamId(),
                        eventTime, location, status, probability, rainRisk, stale));
            }
            // A long overview can outlive roster changes made while another event is loading.
            final User current = data.get(input.username());
            if (current == null) {
                presenter.prepareFailView("Your account is no longer available.");
                return;
            }
            risks.removeIf(risk -> {
                final Event latest = data.getEvent(risk.eventId());
                return latest == null || latest.isCancelled() || !risk.dateTime().equals(latest.getDateTime())
                        || !Objects.equals(risk.location(), latest.getLocation())
                        || !canRead(current, data.getTeam(latest.getTeamId()));
            });
        }
        catch (IllegalStateException | IllegalArgumentException | UncheckedIOException ex) {
            presenter.prepareFailView("Rain risk unavailable: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(new FlagRainRiskOutputData(risks));
    }

    private boolean canRead(User user, Team team) {
        if (team == null) {
            return false;
        }
        final String username = user.getUsername();
        if (user.getRole() == Role.ADMIN
                || user.getRole() == Role.COACH && username.equals(team.getCoachUsername())
                || user.getRole() == Role.PLAYER && team.getPlayerUsernames().contains(username)) {
            return true;
        }
        if (user.getRole() == Role.PARENT) {
            final List<String> linked = data.getLinkedPlayerUsernames(username);
            return linked != null && linked.stream().anyMatch(team.getPlayerUsernames()::contains);
        }
        return false;
    }
}
