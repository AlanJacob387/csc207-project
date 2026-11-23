package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.geom.Rectangle2D;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileAppDataAccessObject;
import entity.Announcement;
import entity.Role;
import entity.Team;
import entity.User;
import interface_adapter.Session;
import interface_adapter.post_announcement.PostAnnouncementController;
import interface_adapter.post_announcement.PostAnnouncementPresenter;
import interface_adapter.post_announcement.PostAnnouncementViewModel;
import interface_adapter.view_announcements.ViewAnnouncementsController;
import interface_adapter.view_announcements.ViewAnnouncementsPresenter;
import interface_adapter.view_announcements.ViewAnnouncementsViewModel;
import use_case.post_announcement.PostAnnouncementInteractor;
import use_case.view_announcements.ViewAnnouncementsInteractor;
import view.AnnouncementsView;

/** Exercises the Swing page through controllers and the real store without opening a window. */
class AnnouncementsFlowTest {
    @TempDir
    Path tempDir;

    private FileAppDataAccessObject store;
    private Session session;
    private PostAnnouncementViewModel posting;
    private ViewAnnouncementsViewModel reading;
    private ViewAnnouncementsController readController;
    private AnnouncementsView view;

    @BeforeEach
    void setUp() throws Exception {
        store = new FileAppDataAccessObject(tempDir.resolve("app-data.json"));
        seedAccounts(store);
        SwingUtilities.invokeAndWait(() -> wirePage(store));
    }

    @Test
    void coachPostsThroughTheFormAndPlayerReadsThePersistedMessage() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));
            draft().setText("  Practice moved to Gym B.\nBring your uniform.  ");

            button("Post announcement").doClick(0);

            assertEquals("Announcement posted.", posting.getSuccessMessage());
            assertEquals("", draft().getText());
            assertEquals(1, reading.getAnnouncements().size());
            assertEquals("Practice moved to Gym B.\nBring your uniform.",
                    reading.getAnnouncements().get(0).getMessage());
            assertTrue(components(view).stream().filter(JTextArea.class::isInstance)
                    .map(JTextArea.class::cast).anyMatch(area -> !area.isEditable()
                            && area.getText().contains("Practice moved to Gym B.")));

            session.clearCurrentUser();
            session.setCurrentUser(new User("player1", "pw", "Player One", Role.PLAYER));

            assertEquals(1, reading.getAnnouncements().size());
            assertFalse(reading.canPost());
            assertFalse(button("Post announcement").isEnabled());
        });
        final FileAppDataAccessObject reopened = new FileAppDataAccessObject(tempDir.resolve("app-data.json"));
        assertEquals(1, reopened.getAnnouncementsForTeam("t1").size());
        assertEquals("keep-password", reopened.get("coach1").getPassword());
        assertEquals(List.of("player1"), reopened.getTeam("t1").getPlayerUsernames());
    }

    @Test
    void correctingAnInvalidPostClearsTheOldError() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));
            draft().setText("   ");
            button("Post announcement").doClick(0);

            assertFalse(posting.getErrorMessage().isEmpty());
            assertTrue(reading.getAnnouncements().isEmpty());
            assertEquals("   ", draft().getText());

            draft().setText("Game starts at six.");
            button("Post announcement").doClick(0);

            assertEquals("", posting.getErrorMessage());
            assertEquals("Announcement posted.", posting.getSuccessMessage());
            assertEquals(1, reading.getAnnouncements().size());
        });
    }

    @Test
    void failedPersistenceKeepsTheDraftAndDoesNotShowSuccess() throws Exception {
        final FileAppDataAccessObject broken = new FileAppDataAccessObject(tempDir.resolve("broken.json")) {
            @Override
            public void saveAnnouncement(Announcement announcement) {
                throw new IllegalStateException("Store unavailable");
            }
        };
        seedAccounts(broken);
        SwingUtilities.invokeAndWait(() -> {
            wirePage(broken);
            session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));
            draft().setText("Keep this draft.");
            button("Post announcement").doClick(0);

            assertEquals("Keep this draft.", draft().getText());
            assertFalse(posting.getErrorMessage().isEmpty());
            assertEquals("", posting.getSuccessMessage());
            assertTrue(reading.getAnnouncements().isEmpty());
        });
    }

    @Test
    void sessionChangesClearThePreviousUsersPostsDraftAndFeedback() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));
            draft().setText("Team one only.");
            button("Post announcement").doClick(0);
            draft().setText("Unsent draft");

            session.clearCurrentUser();

            assertTrue(reading.getAnnouncements().isEmpty());
            assertTrue(reading.getTeams().isEmpty());
            assertEquals("", draft().getText());
            assertEquals("", posting.getSuccessMessage());

            session.setCurrentUser(new User("coach2", "pw", "Coach Two", Role.COACH));
            assertEquals("t2", reading.getSelectedTeamId());
            assertTrue(reading.getAnnouncements().isEmpty());

            session.setCurrentUser(new User("parent1", "pw", "Parent One", Role.PARENT));
            assertTrue(reading.getTeams().isEmpty());
            assertTrue(reading.getAnnouncements().isEmpty());
            assertFalse(button("Post announcement").isEnabled());
            assertFalse(teamChoice().isEnabled());
        });
    }

    @Test
    void deniedTeamReadClearsOldContentAndRefreshRecovers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));
            draft().setText("Team one only.");
            button("Post announcement").doClick(0);

            readController.execute("t2");

            assertTrue(reading.getAnnouncements().isEmpty());
            assertTrue(reading.getTeams().isEmpty());
            assertFalse(reading.getErrorMessage().isEmpty());
            assertFalse(button("Post announcement").isEnabled());

            button("Refresh").doClick(0);

            assertEquals("", reading.getErrorMessage());
            assertEquals("t1", reading.getSelectedTeamId());
            assertEquals(1, reading.getAnnouncements().size());
        });
    }

    @Test
    void aCoachWithMultipleTeamsPostsToTheSelectedTeam() throws Exception {
        store.saveTeam(new Team("t3", "Third Team", "Soccer", "coach1"));
        store.saveAnnouncement(new Announcement("existing", "t3", "coach1", "Third team's post.",
                LocalDateTime.of(2026, 11, 9, 12, 0)));
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));
            assertEquals("t1", reading.getSelectedTeamId());

            teamChoice().setSelectedIndex(1);

            assertEquals("t3", reading.getSelectedTeamId());
            assertEquals("Third team's post.", reading.getAnnouncements().get(0).getMessage());
            draft().setText("A second post for team three.");
            button("Post announcement").doClick(0);

            assertEquals(2, store.getAnnouncementsForTeam("t3").size());
            assertTrue(store.getAnnouncementsForTeam("t1").isEmpty());
            assertEquals("t3", reading.getSelectedTeamId());
        });
    }

    @Test
    void longAnnouncementsRemainReadableWhenTheWindowNarrows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            session.setCurrentUser(new User("coach1", "pw", "Coach One", Role.COACH));
            draft().setText("Practice has moved to the north gym. Bring water and arrive early. ".repeat(18)
                    + "\n\nThe final pickup point is the south entrance.");
            button("Post announcement").doClick(0);

            for (int width : new int[]{1000, 650, 440}) {
                view.setSize(width, 700);
                layoutTree(view);
                layoutTree(view);
                final JTextArea content = components(view).stream().filter(JTextArea.class::isInstance)
                        .map(JTextArea.class::cast).filter(area -> !area.isEditable()).findFirst().orElseThrow();
                final Rectangle2D lastLine = assertDoesNotThrow(
                        () -> content.modelToView2D(content.getDocument().getLength()));

                assertTrue(lastLine.getMaxY() <= content.getHeight(),
                        "The final line must fit in the scrollable content at width " + width);
            }
        });
    }

    private void wirePage(FileAppDataAccessObject dataAccess) {
        session = new Session();
        posting = new PostAnnouncementViewModel();
        reading = new ViewAnnouncementsViewModel();
        final PostAnnouncementController postController = new PostAnnouncementController(
                new PostAnnouncementInteractor(dataAccess, new PostAnnouncementPresenter(posting)), session);
        readController = new ViewAnnouncementsController(
                new ViewAnnouncementsInteractor(dataAccess, new ViewAnnouncementsPresenter(reading)), session);
        view = new AnnouncementsView(postController, posting, readController, reading, session);
    }

    private static void seedAccounts(FileAppDataAccessObject dataAccess) {
        dataAccess.save(new User("coach1", "keep-password", "Coach One", Role.COACH));
        dataAccess.save(new User("coach2", "pw", "Coach Two", Role.COACH));
        dataAccess.save(new User("player1", "pw", "Player One", Role.PLAYER));
        dataAccess.save(new User("parent1", "pw", "Parent One", Role.PARENT));
        final Team first = new Team("t1", "First Team", "Basketball", "coach1");
        first.addPlayer("player1");
        dataAccess.saveTeam(first);
        dataAccess.saveTeam(new Team("t2", "Second Team", "Soccer", "coach2"));
    }

    private JTextArea draft() {
        return components(view).stream().filter(JTextArea.class::isInstance).map(JTextArea.class::cast)
                .filter(area -> "announcementMessage".equals(area.getName())).findFirst().orElseThrow();
    }

    private JComboBox<?> teamChoice() {
        return components(view).stream().filter(JComboBox.class::isInstance).map(JComboBox.class::cast)
                .findFirst().orElseThrow();
    }

    private JButton button(String text) {
        return components(view).stream().filter(JButton.class::isInstance).map(JButton.class::cast)
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static List<Component> components(Container container) {
        final List<Component> result = new ArrayList<>();
        for (Component component : container.getComponents()) {
            result.add(component);
            if (component instanceof Container child) {
                result.addAll(components(child));
            }
        }
        return result;
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) {
                layoutTree(child);
            }
        }
    }
}
