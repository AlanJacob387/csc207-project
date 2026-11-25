package use_case.view_notifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.EventNotification;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/** Exercises recipient authorization independently of navigation and store filtering. */
class ViewNotificationsInteractorTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 23, 17, 0);
    private FakeStore store;
    private FakeCurrentUser currentUser;
    private RecordingPresenter presenter;
    private ViewNotificationsInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        currentUser = new FakeCurrentUser();
        presenter = new RecordingPresenter();
        interactor = new ViewNotificationsInteractor(store, presenter, currentUser);
        for (User user : List.of(user("coach-a", Role.COACH), user("coach-b", Role.COACH),
                user("player-a", Role.PLAYER), user("player-b", Role.PLAYER),
                user("parent", Role.PARENT), user("admin", Role.ADMIN))) {
            store.users.put(user.getUsername(), user);
        }
        store.teams.add(team("a", "Alpha", "coach-a", "player-a"));
        store.teams.add(team("b", "Beta", "coach-b", "player-b"));
        store.teams.add(team("c", "Gamma", "coach-a", "another-child"));
        store.notifications.add(notice("a-created", "a", EventNotification.CREATED, NOW));
        store.notifications.add(notice("b-changed", "b", EventNotification.CHANGED, NOW.minusMinutes(1)));
        store.notifications.add(notice("c-cancelled", "c", EventNotification.CANCELLED, NOW.minusMinutes(2)));
        store.notifications.add(notice("orphan", "missing-team", EventNotification.CREATED, NOW.plusDays(1)));
        currentUser.username = "player-a";
    }

    @Test
    void aPlayerReadsOnlyTheirRosterDespiteAMixedTeamStoreResponse() {
        load(null);

        assertNull(presenter.error);
        assertEquals(List.of("a"), teamIds());
        assertEquals(List.of("a-created"), notificationIds());
        assertEquals(0, store.linkQueries);
    }

    @Test
    void aCoachReadsOnlyTeamsTheyCoach() {
        currentUser.username = "coach-a";
        store.teams.get(1).addPlayer("coach-a");

        load(null);

        assertEquals(List.of("a", "c"), teamIds());
        assertEquals(List.of("a-created", "c-cancelled"), notificationIds());
    }

    @Test
    void anAdminReadsAllExistingTeamsButNotOrphanedNotices() {
        currentUser.username = "admin";

        load(null);

        assertEquals(List.of("a", "b", "c"), teamIds());
        assertEquals(List.of("a-created", "b-changed", "c-cancelled"), notificationIds());
    }

    @Test
    void aParentReadsTeamsContainingAnyOfTheirLinkedChildren() {
        currentUser.username = "parent";
        store.links.put("parent", List.of("player-a", "another-child"));

        load(null);

        assertEquals(List.of("a", "c"), teamIds());
        assertEquals(List.of("a-created", "c-cancelled"), notificationIds());
        assertEquals("parent", store.lastParentQuery);
    }

    @Test
    void aParentWithoutLinksCannotUseRosterMembershipOrTeamSelectionToRead() {
        currentUser.username = "parent";
        store.teams.get(0).addPlayer("parent");

        load(null);
        assertTrue(presenter.success.getTeams().isEmpty());
        assertTrue(presenter.success.getNotifications().isEmpty());
        assertEquals(0, store.feedQueries);

        presenter = new RecordingPresenter();
        interactor = new ViewNotificationsInteractor(store, presenter, currentUser);
        load("a");
        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertEquals(0, store.feedQueries);
    }

    @Test
    void anAccessibleTeamCanBeSelectedWithoutChangingTheReader() {
        currentUser.username = "admin";

        load(" b ");

        assertEquals("b", presenter.success.getSelectedTeamId());
        assertEquals(List.of("b-changed"), notificationIds());
        assertEquals(List.of("a", "b", "c"), teamIds());
    }

    @Test
    void inaccessibleOrUnknownTeamsAreRejectedBeforeReadingTheFeed() {
        for (String selection : List.of("b", "missing-team", "admin")) {
            final RecordingPresenter denied = new RecordingPresenter();
            new ViewNotificationsInteractor(store, denied, currentUser)
                    .execute(new ViewNotificationsInputData(selection));

            assertNotNull(denied.error, selection);
            assertNull(denied.success, selection);
        }
        assertEquals(0, store.feedQueries);
    }

    @Test
    void signedOutAndMissingAccountsCannotReadMembershipOrFeedData() {
        for (String username : new String[]{null, "", " \t", "unknown"}) {
            currentUser.username = username;
            final RecordingPresenter denied = new RecordingPresenter();
            new ViewNotificationsInteractor(store, denied, currentUser).execute(null);

            assertEquals("Sign in to view notifications.", denied.error);
            assertNull(denied.success);
        }
        assertEquals(0, store.teamQueries);
        assertEquals(0, store.feedQueries);
    }

    @Test
    void aUserWithoutATeamGetsAnEmptyFeedWithoutReadingTheLog() {
        store.users.put("unassigned", user("unassigned", Role.PLAYER));
        currentUser.username = "unassigned";

        load(null);

        assertNull(presenter.error);
        assertTrue(presenter.success.getTeams().isEmpty());
        assertTrue(presenter.success.getNotifications().isEmpty());
        assertEquals(0, store.feedQueries);
    }

    @Test
    void anExistingTeamWithoutChangesHasASuccessfulEmptyFeed() {
        store.notifications.clear();

        load(null);

        assertNull(presenter.error);
        assertEquals(List.of("a"), teamIds());
        assertTrue(presenter.success.getNotifications().isEmpty());
    }

    @Test
    void changesAreNewestFirstAndTiesHaveAStableOrderWithAllDisplayFields() {
        store.notifications.clear();
        store.notifications.add(notice("old", "a", EventNotification.CREATED, NOW.minusDays(1)));
        store.notifications.add(notice("z", "a", EventNotification.CANCELLED, NOW));
        store.notifications.add(null);
        store.notifications.add(notice("a", "a", EventNotification.CHANGED, NOW));

        load(null);

        assertEquals(List.of("a", "z", "old"), notificationIds());
        final ViewNotificationsOutputData.NotificationData first = presenter.success.getNotifications().get(0);
        assertEquals("event-a", first.getEventId());
        assertEquals("a", first.getTeamId());
        assertEquals("Alpha", first.getTeamName());
        assertEquals(EventNotification.CHANGED, first.getChangeType());
        assertEquals("Message a", first.getMessage());
        assertEquals(NOW, first.getCreatedAt());
    }

    @Test
    void roleAndRosterChangesAreAppliedAgainOnEveryRead() {
        currentUser.username = "admin";
        load(null);
        assertEquals(3, notificationIds().size());

        store.users.put("admin", user("admin", Role.PLAYER));
        load(null);
        assertTrue(presenter.success.getNotifications().isEmpty());

        store.teams.get(1).addPlayer("admin");
        load(null);
        assertEquals(List.of("b-changed"), notificationIds());
    }

    @Test
    void removedParentLinksTakeEffectOnTheNextRead() {
        currentUser.username = "parent";
        store.links.put("parent", List.of("player-a"));
        load(null);
        assertEquals(List.of("a-created"), notificationIds());

        store.links.clear();
        load(null);

        assertTrue(presenter.success.getTeams().isEmpty());
        assertTrue(presenter.success.getNotifications().isEmpty());
    }

    @Test
    void aDifferentSignedInAccountDoesNotInheritThePreviousAccountsTeams() {
        load(null);
        assertEquals(List.of("a-created"), notificationIds());

        currentUser.username = "player-b";
        load(null);

        assertEquals(List.of("b-changed"), notificationIds());
        assertEquals(List.of("b"), teamIds());
    }

    @Test
    void readFailuresAreReportedRatherThanPresentedAsAnEmptyFeed() {
        for (String stage : List.of("account", "teams", "links", "feed")) {
            store.failureStage = stage;
            currentUser.username = stage.equals("links") ? "parent" : "player-a";
            final RecordingPresenter failed = new RecordingPresenter();

            new ViewNotificationsInteractor(store, failed, currentUser).execute(null);

            assertTrue(failed.error.startsWith("Could not load notifications:"), stage);
            assertNull(failed.success, stage);
        }
    }

    @Test
    void aSessionChangeWhileReadingDoesNotPublishTheOldAccountsNotices() {
        store.onFeedRead = currentUser::clearCurrentUser;

        load(null);

        assertNull(presenter.success);
        assertEquals("Your session changed. Refresh to view notifications.", presenter.error);
    }

    @Test
    void outputCollectionsCannotBeEditedByTheViewOrByTheOriginalCaller() {
        load(null);

        assertThrows(UnsupportedOperationException.class, () -> presenter.success.getTeams().clear());
        assertThrows(UnsupportedOperationException.class, () -> presenter.success.getNotifications().clear());
        final List<ViewNotificationsOutputData.TeamData> teams = new ArrayList<>(presenter.success.getTeams());
        final List<ViewNotificationsOutputData.NotificationData> notices =
                new ArrayList<>(presenter.success.getNotifications());
        final ViewNotificationsOutputData copied = new ViewNotificationsOutputData(teams, "", notices);
        teams.clear();
        notices.clear();
        assertFalse(copied.getTeams().isEmpty());
        assertFalse(copied.getNotifications().isEmpty());
    }

    private void load(String teamId) {
        interactor.execute(new ViewNotificationsInputData(teamId));
    }

    private List<String> teamIds() {
        return presenter.success.getTeams().stream()
                .map(ViewNotificationsOutputData.TeamData::getTeamId).toList();
    }

    private List<String> notificationIds() {
        return presenter.success.getNotifications().stream()
                .map(ViewNotificationsOutputData.NotificationData::getNotificationId).toList();
    }

    private static User user(String username, Role role) {
        return new User(username, "pw", username, role);
    }

    private static Team team(String id, String name, String coach, String player) {
        final Team team = new Team(id, name, "Soccer", coach);
        team.addPlayer(player);
        return team;
    }

    private static EventNotification notice(String id, String teamId, String type, LocalDateTime createdAt) {
        return new EventNotification(id, "event-" + teamId, teamId, type, "Message " + id, createdAt);
    }

    /** Returns all teams and changes, leaving authorization entirely to the use case. */
    private static class FakeStore implements ViewNotificationsDataAccessInterface {
        private final Map<String, User> users = new HashMap<>();
        private final List<Team> teams = new ArrayList<>();
        private final Map<String, List<String>> links = new HashMap<>();
        private final List<EventNotification> notifications = new ArrayList<>();
        private int teamQueries;
        private int feedQueries;
        private int linkQueries;
        private String lastParentQuery;
        private String failureStage;
        private Runnable onFeedRead;

        @Override
        public User get(String username) {
            failIf("account");
            return users.get(username);
        }

        @Override
        public List<Team> getAllTeams() {
            teamQueries++;
            failIf("teams");
            return teams;
        }

        @Override
        public List<String> getLinkedPlayerUsernames(String parentUsername) {
            linkQueries++;
            lastParentQuery = parentUsername;
            failIf("links");
            return links.getOrDefault(parentUsername, List.of());
        }

        @Override
        public List<EventNotification> getEventNotifications() {
            feedQueries++;
            failIf("feed");
            if (onFeedRead != null) {
                onFeedRead.run();
            }
            return notifications;
        }

        private void failIf(String stage) {
            if (stage.equals(failureStage)) {
                if (stage.equals("teams")) {
                    throw new UncheckedIOException(new IOException("Team store is unavailable"));
                }
                throw new IllegalStateException("Cannot read " + stage);
            }
        }
    }

    /** Holds identity without coupling these business tests to a session adapter. */
    private static class FakeCurrentUser implements CurrentUserInterface {
        private entity.Role currentRole;

        @Override
        public entity.Role getCurrentRole() {
            return currentRole;
        }

        private String username;

        @Override
        public String getCurrentUsername() {
            return username;
        }

        @Override
        public void setCurrentUser(User user) {
            currentRole = user.getRole();
            username = user.getUsername();
        }

        @Override
        public void clearCurrentUser() {
            username = "";
        }
    }

    private static class RecordingPresenter implements ViewNotificationsOutputBoundary {
        private ViewNotificationsOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ViewNotificationsOutputData outputData) {
            success = outputData;
            error = null;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            success = null;
            error = errorMessage;
        }
    }
}
