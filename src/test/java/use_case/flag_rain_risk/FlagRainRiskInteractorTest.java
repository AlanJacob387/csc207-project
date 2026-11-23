package use_case.flag_rain_risk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.view_event_weather.EventWeatherGateway;
import use_case.view_event_weather.WeatherForecast;

class FlagRainRiskInteractorTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);
    @Test
    void sixtyIsRiskFiftyNineIsSafeAndMissingDataIsUnavailable() {
        final Store store = new Store();
        final Result result = new Result();
        store.add("safe", 1);
        store.add("risky", 2);
        store.add("missing", 3);
        new FlagRainRiskInteractor(store, result, (location, date) -> {
            if (date.getDayOfMonth() == 26) {
                throw new IllegalStateException("No rain probability");
            }
            return new WeatherForecast(date, 10, date.getDayOfMonth() == 24 ? 59 : 60, location, CLOCK.instant(), false);
        }, CLOCK).execute(new FlagRainRiskInputData("player", null));
        assertNull(result.error);
        assertEquals(3, result.data.events().size());
        assertFalse(result.data.events().get(0).rainRisk());
        assertTrue(result.data.events().get(1).rainRisk());
        assertNull(result.data.events().get(2).precipitationProbabilityPercent());
        assertTrue(result.data.events().get(2).status().startsWith("Unavailable:"));
    }

    @Test
    void cancelledAndPastEventsAreExcludedAndOutsideWindowDoesNotCallTheProvider() {
        final Store store = new Store();
        final Result result = new Result();
        store.add("cancelled", 1).cancel();
        store.add("past", -1);
        store.add("distant", 16);
        final AtomicInteger calls = new AtomicInteger();
        new FlagRainRiskInteractor(store, result, (location, date) -> {
            calls.incrementAndGet();
            throw new IllegalStateException("unexpected");
        }, CLOCK).execute(new FlagRainRiskInputData("coach", "team"));
        assertEquals(1, result.data.events().size());
        assertEquals(0, calls.get());
        assertNull(result.data.events().get(0).precipitationProbabilityPercent());
    }

    @Test
    void unknownReaderOrForbiddenTeamIsDeniedBeforeNetwork() {
        final Store store = new Store();
        final Result result = new Result();
        store.add("event", 1);
        final AtomicInteger calls = new AtomicInteger();
        final EventWeatherGateway weather = (location, date) -> {
            calls.incrementAndGet();
            throw new IllegalStateException();
        };
        for (String user : List.of("missing", "other")) {
            new FlagRainRiskInteractor(store, result, weather, CLOCK).execute(new FlagRainRiskInputData(user, "team"));
            assertNotNull(result.error);
        }
        assertEquals(0, calls.get());
    }

    @Test
    void linkedParentReadsOnlyTheirChildsTeamAndFlagsStaleRiskExplicitly() {
        final Store store = new Store();
        final Result result = new Result();
        store.add("event", 1);
        new FlagRainRiskInteractor(store, result, (location, date) ->
                new WeatherForecast(date, 10, 60, location, CLOCK.instant(), true), CLOCK)
                .execute(new FlagRainRiskInputData("parent", null));
        assertEquals(1, result.data.events().size());
        assertTrue(result.data.events().get(0).stale());
        assertTrue(result.data.events().get(0).status().contains("stale"));
        store.links.clear();
        new FlagRainRiskInteractor(store, result, (location, date) -> {
            fail("Unlinked parent used network");
            return null;
        }, CLOCK)
                .execute(new FlagRainRiskInputData("parent", null));
        assertTrue(result.data.events().isEmpty());
    }

    @Test
    void previouslyLoadedRowsAreReauthorizedBeforeTheWholeOverviewIsReturned() {
        final Store store = new Store();
        final Result result = new Result();
        store.add("one", 1);
        store.add("two", 2);
        new FlagRainRiskInteractor(store, result, (location, date) -> {
            if (date.getDayOfMonth() == 25) {
                store.team.removePlayer("player");
            }
            return new WeatherForecast(date, 10, 90, location, CLOCK.instant(), false);
        }, CLOCK).execute(new FlagRainRiskInputData("player", null));
        assertTrue(result.data.events().isEmpty());
    }

    static final class Store implements FlagRainRiskDataAccessInterface {
        final Map<String, User> users = new HashMap<>();
        final List<Event> events = new ArrayList<>();
        final Team team = new Team("team", "Team", "Soccer", "coach");
        final List<String> links = new ArrayList<>(List.of("player"));
        Store() {
            for (Role role : Role.values()) {
                String name = role.name().toLowerCase();
                users.put(name, new User(name, "pw", name, role));
            }
            users.put("other", new User("other", "pw", "Other", Role.PLAYER));
            team.addPlayer("player");
        }
        Event add(String id, int offset) { Event event = new Event(id, "team", Event.EventType.PRACTICE,
                LocalDateTime.now(CLOCK).plusDays(offset), "Toronto", null); events.add(event); return event; }
        public User get(String username) {
            return users.get(username);
        }
        public Event getEvent(String id) {
            return events.stream().filter(event -> id.equals(event.getEventId())).findFirst().orElse(null);
        }
        public Team getTeam(String id) {
            return "team".equals(id) ? team : null;
        }
        public List<Event> getAllEvents() {
            return List.copyOf(events);
        }
        public List<String> getLinkedPlayerUsernames(String username) {
            return List.copyOf(links);
        }
    }

    static final class Result implements FlagRainRiskOutputBoundary {
        FlagRainRiskOutputData data;
        String error;
        public void prepareSuccessView(FlagRainRiskOutputData value) {
            data = value;
            error = null;
        }
        public void prepareFailView(String value) {
            error = value;
            data = null;
        }
    }
}
