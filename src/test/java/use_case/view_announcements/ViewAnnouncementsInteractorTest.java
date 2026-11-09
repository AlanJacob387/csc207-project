package use_case.view_announcements;

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
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import entity.Announcement;
import entity.Role;
import entity.Team;
import entity.User;

/** Verifies team isolation, empty states, post ordering, and failed reads. */
class ViewAnnouncementsInteractorTest {
    private FakeStore store;
    private RecordingPresenter presenter;
    private ViewAnnouncementsInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        for (Role role : Role.values()) {
            final String username = role.name().toLowerCase();
            store.users.put(username, new User(username, "pw", username, role));
        }
        final Team owned = new Team("owned", "Raptors", "Basketball", "coach");
        owned.addPlayer("player");
        store.teams.add(owned);
        store.teams.add(new Team("other", "Bears", "Soccer", "other-coach"));
        presenter = new RecordingPresenter();
        interactor = new ViewAnnouncementsInteractor(store, presenter);
    }

    @Test
    void anAssignedCoachCanReadAndPostToTheirTeam() {
        store.posts.add(post("first", "owned", LocalDateTime.of(2026, 11, 9, 12, 0)));

        interactor.execute(new ViewAnnouncementsInputData("coach", null));

        assertNull(presenter.error);
        assertEquals(List.of("owned"), teamIds());
        assertEquals("owned", presenter.success.getSelectedTeamId());
        assertTrue(presenter.success.canPost());
        assertEquals("coach", presenter.success.getAnnouncements().get(0).getAuthorUsername());
    }

    @Test
    void aPlayerReadsOnlyTheirRosterTeamsWithoutPostingPermission() {
        interactor.execute(new ViewAnnouncementsInputData("player", " owned "));

        assertNull(presenter.error);
        assertEquals(List.of("owned"), teamIds());
        assertEquals("owned", presenter.success.getSelectedTeamId());
        assertTrue(presenter.success.getAnnouncements().isEmpty());
        assertFalse(presenter.success.canPost());
    }

    @Test
    void anAdminReadsAllTeamsWithoutCoachPostingPermission() {
        interactor.execute(new ViewAnnouncementsInputData("admin", "owned"));

        assertNull(presenter.error);
        assertEquals(List.of("other", "owned"), teamIds());
        assertFalse(presenter.success.canPost());
    }

    @Test
    void automaticSelectionUsesTeamNameThenIdForAStableOrder() {
        store.teams.add(new Team("alpha-2", "Alpha", "Soccer", "coach"));
        store.teams.add(new Team("alpha-1", "Alpha", "Soccer", "coach"));

        interactor.execute(new ViewAnnouncementsInputData("coach", "  "));

        assertEquals(List.of("alpha-1", "alpha-2", "owned"), teamIds());
        assertEquals("alpha-1", presenter.success.getSelectedTeamId());
        assertEquals(List.of("alpha-1"), store.postQueries);
    }

    @Test
    void postsAreNewestFirstWithAnIdTieBreakAndForeignPostsAreExcluded() {
        final LocalDateTime newest = LocalDateTime.of(2026, 11, 9, 18, 0);
        store.posts.add(post("old", "owned", newest.minusDays(1)));
        store.posts.add(post("b", "owned", newest));
        store.posts.add(post("private", "other", newest.plusDays(1)));
        store.posts.add(post("a", "owned", newest));

        interactor.execute(new ViewAnnouncementsInputData("player", "owned"));

        assertNull(presenter.error);
        assertEquals(List.of("a", "b", "old"), presenter.success.getAnnouncements().stream()
                .map(ViewAnnouncementsOutputData.AnnouncementData::getAnnouncementId).toList());
        assertEquals("Message a", presenter.success.getAnnouncements().get(0).getMessage());
        assertEquals(newest, presenter.success.getAnnouncements().get(0).getCreatedAt());
    }

    @Test
    void aParentGetsAnEmptyStateUntilParentLinksAreAvailable() {
        interactor.execute(new ViewAnnouncementsInputData("parent", null));

        assertNull(presenter.error);
        assertTrue(presenter.success.getTeams().isEmpty());
        assertNull(presenter.success.getSelectedTeamId());
        assertTrue(presenter.success.getAnnouncements().isEmpty());
        assertFalse(presenter.success.canPost());
        assertEquals(0, store.teamQueries);
        assertTrue(store.postQueries.isEmpty());
    }

    @Test
    void aParentCannotUseAnExplicitTeamToBypassTheEmptyState() {
        interactor.execute(new ViewAnnouncementsInputData("parent", "owned"));

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertTrue(store.postQueries.isEmpty());
    }

    @Test
    void unknownAndInaccessibleSelectionsFailWithoutReadingPosts() {
        for (String username : List.of("coach", "player")) {
            for (String teamId : List.of("other", "missing")) {
                final RecordingPresenter denied = new RecordingPresenter();

                new ViewAnnouncementsInteractor(store, denied)
                        .execute(new ViewAnnouncementsInputData(username, teamId));

                assertNotNull(denied.error, username + " / " + teamId);
                assertNull(denied.success);
            }
        }
        assertTrue(store.postQueries.isEmpty());
    }

    @Test
    void aReaderWithNoAccessibleTeamsGetsASuccessfulEmptyState() {
        store.users.put("unassigned", new User("unassigned", "pw", "No Team", Role.PLAYER));

        interactor.execute(new ViewAnnouncementsInputData("unassigned", null));

        assertNull(presenter.error);
        assertTrue(presenter.success.getTeams().isEmpty());
        assertTrue(presenter.success.getAnnouncements().isEmpty());
        assertNull(presenter.success.getSelectedTeamId());
        assertFalse(presenter.success.canPost());
        assertTrue(store.postQueries.isEmpty());
    }

    @Test
    void aDeletedAccountIsDeniedBeforeTeamAndPostQueries() {
        interactor.execute(new ViewAnnouncementsInputData("missing", "owned"));

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertEquals(0, store.teamQueries);
        assertTrue(store.postQueries.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void aMissingIdentityIsDenied(ViewAnnouncementsInputData request) {
        interactor.execute(request);

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertEquals(0, store.teamQueries);
        assertTrue(store.postQueries.isEmpty());
    }

    private static Stream<ViewAnnouncementsInputData> invalidRequests() {
        return Stream.of(null, new ViewAnnouncementsInputData(null, "owned"),
                new ViewAnnouncementsInputData(" \t", "owned"));
    }

    @Test
    void aStoredRoleChangeIsAppliedOnTheNextRequest() {
        interactor.execute(new ViewAnnouncementsInputData("coach", "owned"));
        store.users.put("coach", new User("coach", "pw", "Parent Now", Role.PARENT));
        presenter.success = null;

        interactor.execute(new ViewAnnouncementsInputData("coach", "owned"));

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertEquals(List.of("owned"), store.postQueries);
    }

    @Test
    void accountAndTeamReadFailuresArePresentedWithoutReadingPosts() {
        for (String stage : List.of("account", "teams")) {
            store.failureStage = stage;
            final RecordingPresenter failed = new RecordingPresenter();

            new ViewAnnouncementsInteractor(store, failed)
                    .execute(new ViewAnnouncementsInputData("coach", "owned"));

            assertTrue(failed.error.startsWith("Could not load announcements:"));
            assertNull(failed.success);
        }
        assertTrue(store.postQueries.isEmpty());
    }

    @Test
    void aPostReadFailureIsPresentedInsteadOfAnEmptyList() {
        store.failureStage = "posts";

        interactor.execute(new ViewAnnouncementsInputData("coach", "owned"));

        assertTrue(presenter.error.startsWith("Could not load announcements:"));
        assertNull(presenter.success);
    }

    @Test
    void presentedCollectionsCannotBeChangedByTheView() {
        interactor.execute(new ViewAnnouncementsInputData("coach", "owned"));

        assertThrows(UnsupportedOperationException.class, () -> presenter.success.getTeams().clear());
        assertThrows(UnsupportedOperationException.class, () -> presenter.success.getAnnouncements().clear());
    }

    private List<String> teamIds() {
        return presenter.success.getTeams().stream()
                .map(ViewAnnouncementsOutputData.TeamData::getTeamId).toList();
    }

    private static Announcement post(String id, String teamId, LocalDateTime createdAt) {
        return new Announcement(id, teamId, "coach", "Message " + id, createdAt);
    }

    /** Deliberately returns mixed-team posts to verify the use case's own filtering. */
    private static class FakeStore implements ViewAnnouncementsDataAccessInterface {
        private final Map<String, User> users = new HashMap<>();
        private final List<Team> teams = new ArrayList<>();
        private final List<Announcement> posts = new ArrayList<>();
        private final List<String> postQueries = new ArrayList<>();
        private int teamQueries;
        private String failureStage;

        @Override
        public User get(String username) {
            if ("account".equals(failureStage)) {
                throw new IllegalStateException("invalid store");
            }
            return users.get(username);
        }

        @Override
        public List<Team> getAllTeams() {
            teamQueries++;
            if ("teams".equals(failureStage)) {
                throw new UncheckedIOException(new IOException("cannot read teams"));
            }
            return teams;
        }

        @Override
        public List<Announcement> getAnnouncementsForTeam(String teamId) {
            postQueries.add(teamId);
            if ("posts".equals(failureStage)) {
                throw new IllegalStateException("cannot read posts");
            }
            return posts;
        }
    }

    /** Records what would be shown without depending on an interface adapter. */
    private static class RecordingPresenter implements ViewAnnouncementsOutputBoundary {
        private ViewAnnouncementsOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ViewAnnouncementsOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
