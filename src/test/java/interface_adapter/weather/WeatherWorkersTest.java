package interface_adapter.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.awt.Component;
import java.awt.Container;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import interface_adapter.Session;
import interface_adapter.view_event_weather.ViewEventWeatherController;
import interface_adapter.view_event_weather.ViewEventWeatherViewModel;
import interface_adapter.flag_rain_risk.FlagRainRiskController;
import interface_adapter.flag_rain_risk.FlagRainRiskViewModel;
import use_case.view_event_weather.ViewEventWeatherDataAccessInterface;
import use_case.view_event_weather.WeatherForecast;
import use_case.flag_rain_risk.FlagRainRiskDataAccessInterface;
import view.EventWeatherPanel;
import view.RainRiskPanel;

/** Exercises actual worker completion, explicit lookup actions, and identity/event cancellation. */
class WeatherWorkersTest {
    @Test
    void showingAnEventDoesNotUseNetworkAndLookupRunsOffTheEventThread() throws Exception {
        final Store store = new Store();
        final Session session = new Session();
        final ViewEventWeatherViewModel model = new ViewEventWeatherViewModel();
        final AtomicInteger calls = new AtomicInteger();
        final AtomicBoolean onEdt = new AtomicBoolean();
        final EventWeatherPanel[] panel = new EventWeatherPanel[1];
        final CountDownLatch described = new CountDownLatch(1);
        final CountDownLatch fetched = new CountDownLatch(1);
        model.addPropertyChangeListener(event -> {
            if (model.getData() != null) {
                if (model.getData().forecast() == null) {
                    described.countDown();
                }
                else {
                    fetched.countDown();
                }
            }
        });
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(store.user);
            panel[0] = new EventWeatherPanel(new ViewEventWeatherController(store, (location, date) -> {
                calls.incrementAndGet();
                onEdt.set(SwingUtilities.isEventDispatchThread());
                return forecast(location, date, 60);
            }, model, session), model, session);
            panel[0].showEvent("one");
        });
        assertTrue(described.await(3, TimeUnit.SECONDS));
        assertEquals(0, calls.get());
        SwingUtilities.invokeAndWait(() -> button(panel[0], "Check weather").doClick(0));
        assertTrue(fetched.await(3, TimeUnit.SECONDS));
        assertEquals(1, calls.get());
        assertFalse(onEdt.get());
        SwingUtilities.invokeAndWait(() -> assertTrue(text(panel[0]).contains("60%")));
    }

    @Test
    void anOldWeatherResponseCannotReplaceTheNewlySelectedEvent() throws Exception {
        final Store store = new Store();
        final Session session = new Session();
        final ViewEventWeatherViewModel model = new ViewEventWeatherViewModel();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch oldFinished = new CountDownLatch(1);
        final CountDownLatch described = new CountDownLatch(1);
        final EventWeatherPanel[] panel = new EventWeatherPanel[1];
        model.addPropertyChangeListener(event -> {
            if (model.getData() != null && model.getData().eventId().equals("two")) {
                described.countDown();
            }
        });
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(store.user);
            panel[0] = new EventWeatherPanel(new ViewEventWeatherController(store, (location, date) -> {
                entered.countDown();
                awaitIgnoringInterrupt(release);
                oldFinished.countDown();
                return forecast(location, date, 90);
            }, model, session), model, session);
            panel[0].showEvent("one");
        });
        final CountDownLatch firstDescribed = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            if (model.getData() != null) {
                firstDescribed.countDown();
            }
            else {
                model.addPropertyChangeListener(event -> {
                    if (model.getData() != null) {
                        firstDescribed.countDown();
                    }
                });
            }
        });
        assertTrue(firstDescribed.await(3, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> button(panel[0], "Check weather").doClick(0));
        assertTrue(entered.await(3, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> panel[0].showEvent("two"));
        assertTrue(described.await(3, TimeUnit.SECONDS));
        release.countDown();
        assertTrue(oldFinished.await(3, TimeUnit.SECONDS));
        settleSwing();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("two", model.getData().eventId());
            assertNull(model.getData().forecast());
            assertFalse(text(panel[0]).contains("90%"));
        });
    }

    @Test
    void loggingOutDuringRainLookupClearsFlagsAndDropsTheLateResponse() throws Exception {
        final Store store = new Store();
        final Session session = new Session();
        final FlagRainRiskViewModel model = new FlagRainRiskViewModel();
        final AtomicReference<Map<String, String>> flags = new AtomicReference<>(Map.of());
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch finished = new CountDownLatch(1);
        final RainRiskPanel[] panel = new RainRiskPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(store.user);
            panel[0] = new RainRiskPanel(new FlagRainRiskController(store, (location, date) -> {
                assertFalse(SwingUtilities.isEventDispatchThread());
                entered.countDown();
                awaitIgnoringInterrupt(release);
                finished.countDown();
                return forecast(location, date, 90);
            }, model, session), model, session);
            panel[0].setOnFlags(flags::set);
            panel[0].refresh();
        });
        assertTrue(entered.await(3, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            session.clearCurrentUser();
            session.setCurrentUser(store.user);
        });
        release.countDown();
        assertTrue(finished.await(3, TimeUnit.SECONDS));
        settleSwing();
        SwingUtilities.invokeAndWait(() -> {
            assertNull(model.getData());
            assertTrue(flags.get().isEmpty());
        });
    }

    private static WeatherForecast forecast(String location, LocalDate date, int probability) {
        return new WeatherForecast(date, 12, probability, location, Instant.now(), false);
    }

    private static void awaitIgnoringInterrupt(CountDownLatch latch) {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (latch.getCount() > 0 && System.nanoTime() < deadline) {
            try {
                latch.await(100, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ignored) {
            }
        }
    }

    private static void settleSwing() throws Exception {
        final CountDownLatch settled = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            final javax.swing.Timer timer = new javax.swing.Timer(100, event -> settled.countDown());
            timer.setRepeats(false);
            timer.start();
        });
        assertTrue(settled.await(3, TimeUnit.SECONDS));
    }

    private static JButton button(Container parent, String title) {
        return components(parent).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> title.equals(button.getText())).findFirst().orElseThrow();
    }

    private static String text(Container parent) {
        return components(parent).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                .map(JTextArea::getText).reduce("", (left, right) -> left + right);
    }

    private static java.util.List<Component> components(Container parent) {
        final java.util.List<Component> found = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            found.add(component); if (component instanceof Container child) {
                found.addAll(components(child));
            }
        }
        return found;
    }

    private static final class Store implements ViewEventWeatherDataAccessInterface, FlagRainRiskDataAccessInterface {
        final User user = new User("coach", "pw", "Coach", Role.COACH);
        final Team team = new Team("team", "Team", "Soccer", "coach");
        final Event first = new Event("one", "team", Event.EventType.PRACTICE,
                LocalDateTime.now().plusDays(1), "Toronto", null);
        final Event second = new Event("two", "team", Event.EventType.GAME,
                LocalDateTime.now().plusDays(2), "Ottawa", "Bears");
        public User get(String username) {
            return user.getUsername().equals(username) ? user : null;
        }
        public Event getEvent(String id) {
            return "one".equals(id) ? first : "two".equals(id) ? second : null;
        }
        public Team getTeam(String id) {
            return "team".equals(id) ? team : null;
        }
        public java.util.List<Event> getAllEvents() {
            return java.util.List.of(first, second);
        }
        public java.util.List<String> getLinkedPlayerUsernames(String username) {
            return java.util.List.of();
        }
    }
}
