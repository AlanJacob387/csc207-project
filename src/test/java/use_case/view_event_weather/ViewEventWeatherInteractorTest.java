package use_case.view_event_weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;

class ViewEventWeatherInteractorTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);
    private Store store;
    private Result result;
    private AtomicInteger calls;
    private EventWeatherGateway gateway;
    @BeforeEach
    void setUp() {
        store = new Store();
        result = new Result();
        calls = new AtomicInteger();
        gateway = (location, date) -> {
            calls.incrementAndGet();
            return new WeatherForecast(date, 10, 60, location, CLOCK.instant(), false);
        };
    }

    @Test
    void assignedCoachGetsAUsableForecastAndMetadataRequestsDoNotUseTheNetwork() {
        execute("coach", false, gateway);
        assertNotNull(result.data);
        assertNull(result.data.forecast());
        assertEquals(0, calls.get());
        execute("coach", true, gateway);
        assertTrue(result.data.forecast().isRainRisk());
        assertEquals(1, calls.get());
    }

    @Test
    void rosterPlayerAdminAndLinkedParentCanReadButOtherReadersCannot() {
        for (String username : List.of("player", "admin", "parent")) {
            execute(username, true, gateway);
            assertNotNull(result.data, username);
        }
        for (String username : List.of("other", "missing")) {
            result = new Result();
            execute(username, true, gateway);
            assertNotNull(result.error, username);
        }
        store.links.clear();
        result = new Result();
        execute("parent", true, gateway);
        assertNotNull(result.error);
        assertEquals(3, calls.get());
    }

    @Test
    void cancelledPastAndTooDistantEventsHaveExplicitUnavailableStates() {
        store.event.cancel();
        execute("coach", true, gateway);
        assertFalse(result.data.canCheck());
        store = new Store();
        for (LocalDateTime time : List.of(LocalDateTime.now(CLOCK).minusDays(1), LocalDateTime.now(CLOCK).plusDays(16))) {
            store.event.setDateTime(time);
            execute("coach", true, gateway);
            assertFalse(result.data.canCheck());
            assertNull(result.data.forecast());
        }
        assertEquals(0, calls.get());
    }

    @Test
    void failedProviderAndInvalidRequestNeverPresentAUsableForecast() {
        execute("coach", true, (location, date) -> {
            throw new IllegalStateException("offline");
        });
        assertNotNull(result.error);
        assertNull(result.data);
        new ViewEventWeatherInteractor(store, result, gateway, CLOCK).execute(null);
        assertNotNull(result.error);
    }

    @Test
    void accessRevokedWhileWeatherLoadsCannotLeakTheResult() {
        execute("player", true, (location, date) -> {
            store.team.removePlayer("player");
            return new WeatherForecast(date, 10, 60, location, CLOCK.instant(), false);
        });
        assertNotNull(result.error);
        assertNull(result.data);
    }

    @Test
    void editingOrCancellingTheEventWhileWeatherLoadsRejectsTheOldResult() {
        execute("coach", true, (location, date) -> {
            store.event.setLocation("Different venue");
            return new WeatherForecast(date, 10, 60, location, CLOCK.instant(), false);
        });
        assertNotNull(result.error);
        assertNull(result.data);
    }

    @Test
    void staleForecastIsExplicitAndDateMismatchIsRejected() {
        execute("coach", true, (location, date) -> new WeatherForecast(date, 9, 59, location, CLOCK.instant(), true));
        assertTrue(result.data.forecast().stale());
        assertFalse(result.data.forecast().isRainRisk());
        result = new Result();
        execute("coach", true, (location, date) -> new WeatherForecast(date.plusDays(1), 9, 59, location, CLOCK.instant(), false));
        assertNotNull(result.error);
    }

    private void execute(String username, boolean fetch, EventWeatherGateway weather) {
        new ViewEventWeatherInteractor(store, result, weather, CLOCK)
                .execute(new ViewEventWeatherInputData(username, "event", "Toronto, Ontario", fetch));
    }

    static final class Store implements ViewEventWeatherDataAccessInterface {
        final Map<String, User> users = new HashMap<>();
        final Team team = new Team("team", "Team", "Soccer", "coach");
        final Event event = new Event("event", "team", Event.EventType.PRACTICE,
                LocalDateTime.of(2026, 11, 24, 18, 0), "Toronto, Ontario", null);
        final List<String> links = new ArrayList<>(List.of("player"));
        Store() {
            for (Role role : Role.values()) {
                final String name = role.name().toLowerCase();
                users.put(name, new User(name, "pw", name, role));
            }
            users.put("other", new User("other", "pw", "Other", Role.PLAYER));
            team.addPlayer("player");
        }
        public User get(String username) {
            return users.get(username);
        }
        public Event getEvent(String id) {
            return "event".equals(id) ? event : null;
        }
        public Team getTeam(String id) {
            return "team".equals(id) ? team : null;
        }
        public List<String> getLinkedPlayerUsernames(String parent) {
            return List.copyOf(links);
        }
    }

    static final class Result implements ViewEventWeatherOutputBoundary {
        ViewEventWeatherOutputData data;
        String error;
        public void prepareSuccessView(ViewEventWeatherOutputData value) {
            data = value;
            error = null;
        }
        public void prepareFailView(String value) {
            error = value;
            data = null;
        }
    }
}
