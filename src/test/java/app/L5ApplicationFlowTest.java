package app;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.User;
import view.AbsenceNoticesView;
import view.CreateEventView;
import view.LoginView;
import view.NotificationsView;
import view.ParentLinksView;
import view.CalendarView;
import view.EventDetailsView;
import view.EventsListView;

/** Exercises the production entry-point wiring with isolated data and real Swing actions. */
class L5ApplicationFlowTest {
    @TempDir
    Path directory;

    @Test
    void aParentSubmitsAnAbsenceAndTheCoachReadsItAfterSigningIn() throws Exception {
        final FileAppDataAccessObject store = new FileAppDataAccessObject(directory.resolve("app.json"));
        SwingUtilities.invokeAndWait(() -> {
            final JPanel app = Main.configure(new AppBuilder(store)).buildContent();
            login(app, "parent1");
            final AbsenceNoticesView absence = child(app, AbsenceNoticesView.class);
            named(absence, "absenceReason", JTextArea.class).setText("Dentist appointment after school.");

            button(absence, "Save absence notice").doClick(0);

            assertTrue(store.getAbsenceNotices().stream()
                    .anyMatch(notice -> notice.getReason().equals("Dentist appointment after school.")));
            assertEquals("", named(absence, "absenceReason", JTextArea.class).getText());
            button(app, "Log out").doClick(0);
            login(app, "coach1");
            assertTrue(named(absence, "absenceFeed", JTextArea.class).getText()
                    .contains("Dentist appointment after school."));
            assertFalse(button(absence, "Save absence notice").isEnabled());
        });
        assertEquals(2, new FileAppDataAccessObject(directory.resolve("app.json")).getAbsenceNotices().size());
    }

    @Test
    void creatingAnEventFromTheRealFormNotifiesItsLinkedParentsAndKeepsOtherTeamsPrivate() throws Exception {
        final FileAppDataAccessObject store = new FileAppDataAccessObject(directory.resolve("app.json"));
        SwingUtilities.invokeAndWait(() -> {
            final JPanel app = Main.configure(new AppBuilder(store)).buildContent();
            login(app, "coach1");
            final CreateEventView form = child(app, CreateEventView.class);
            final List<JTextField> fields = components(form).stream().filter(JTextField.class::isInstance)
                    .map(JTextField.class::cast).toList();
            fields.get(0).setText("demo-team");
            fields.get(1).setText(LocalDateTime.now().plusDays(2)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            fields.get(2).setText("Kingston, Ontario");

            button(form, "Create Event").doClick(0);

            assertEquals(43, store.getAllEvents().size());
            assertEquals(43, store.getEventNotifications().size());
            button(app, "Log out").doClick(0);
            login(app, "parent1");
            final NotificationsView feed = child(app, NotificationsView.class);
            button(feed, "Refresh").doClick(0);
            assertTrue(named(feed, "notificationFeed", JTextArea.class).getText().contains("Kingston, Ontario"));

            button(app, "Log out").doClick(0);
            login(app, "player4");
            button(feed, "Refresh").doClick(0);
            assertFalse(named(feed, "notificationFeed", JTextArea.class).getText().contains("Kingston, Ontario"));
        });
    }

    @Test
    void adminLinkRevocationUpdatesParentAccessAndStaleRosterEntriesCannotBypassIt() throws Exception {
        final FileAppDataAccessObject store = new FileAppDataAccessObject(directory.resolve("app.json"));
        SwingUtilities.invokeAndWait(() -> {
            final JPanel app = Main.configure(new AppBuilder(store)).buildContent();
            login(app, "admin1");
            final ParentLinksView links = child(app, ParentLinksView.class);

            button(links, "Unlink accounts").doClick(0);

            assertFalse(store.getLinkedPlayerUsernames("parent1").contains("player1"));
            store.deleteParentPlayerLink("parent1", "player2");
            button(app, "Log out").doClick(0);
            login(app, "parent1");
            final AbsenceNoticesView absence = child(app, AbsenceNoticesView.class);
            assertFalse(button(absence, "Save absence notice").isEnabled());
            assertTrue(store.getTeamIdsForMember("parent1").isEmpty());

            store.save(new User("player1", "pw", "Former Player", Role.PARENT));
            assertTrue(store.getTeamIdsForMember("player1").isEmpty());
            assertFalse(store.canViewEvent("player1", "demo-team"));
        });
    }

    @Test
    void anEventEditInvalidatesPublishedCalendarAndListForecastFlags() throws Exception {
        final FileAppDataAccessObject store = new FileAppDataAccessObject(directory.resolve("app.json"));
        SwingUtilities.invokeAndWait(() -> {
            final AppBuilder builder = Main.configure(new AppBuilder(store));
            final JPanel app = builder.buildContent();
            login(app, "coach1");
            final CalendarView calendar = child(app, CalendarView.class);
            final EventsListView events = child(app, EventsListView.class);
            calendar.refresh();
            events.refresh();
            final java.util.Map<String, String> flags = java.util.Map.of("demo-demo-team-1", "Rain risk 80%");
            calendar.setRainRiskFlags(flags);
            events.setRainRiskFlags(flags);
            assertTrue(components(calendar).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .anyMatch(label -> label.getText().contains("Rain risk 80%")));

            final EventDetailsView details = builder.getEventDetailsView();
            details.loadEvent("demo-demo-team-1");
            button(details, "Edit").doClick(0);
            named(details, "eventEditLocation", JTextField.class).setText("Kingston, Ontario");
            button(details, "Save").doClick(0);

            assertEquals("Kingston, Ontario", store.getEvent("demo-demo-team-1").getLocation());
            assertFalse(components(calendar).stream().filter(JLabel.class::isInstance).map(JLabel.class::cast)
                    .anyMatch(label -> label.getText().contains("Rain risk 80%")));
        });
    }

    private static void login(Container app, String username) {
        final LoginView login = child(app, LoginView.class);
        components(login).stream().filter(component -> component instanceof JTextField
                && !(component instanceof JPasswordField)).map(JTextField.class::cast)
                .findFirst().orElseThrow().setText(username);
        child(login, JPasswordField.class).setText("pw");
        button(login, "Log In").doClick(0);
    }

    private static JButton button(Container parent, String text) {
        return components(parent).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static <T extends Component> T named(Container parent, String name, Class<T> type) {
        return components(parent).stream().filter(type::isInstance).map(type::cast)
                .filter(component -> name.equals(component.getName())).findFirst().orElseThrow();
    }

    private static <T extends Component> T child(Container parent, Class<T> type) {
        return components(parent).stream().filter(type::isInstance).map(type::cast).findFirst().orElseThrow();
    }

    private static List<Component> components(Container parent) {
        final List<Component> result = new ArrayList<>();
        for (Component component : parent.getComponents()) {
            result.add(component);
            if (component instanceof Container container) {
                result.addAll(components(container));
            }
        }
        return result;
    }
}
