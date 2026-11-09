package use_case.post_announcement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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

/** Exercises posting permissions, validation, and storage failure reporting. */
class PostAnnouncementInteractorTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-09T18:30:00Z"), ZoneOffset.UTC);

    private FakeStore store;
    private RecordingPresenter presenter;
    private PostAnnouncementInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        store.users.put("coach1", new User("coach1", "pw", "Coach One", Role.COACH));
        store.teams.put("team1", new Team("team1", "Raptors", "Basketball", "coach1"));
        presenter = new RecordingPresenter();
        interactor = new PostAnnouncementInteractor(store, presenter, CLOCK);
    }

    @Test
    void theAssignedCoachPostsTrimmedContentWithTheRecordedTime() {
        interactor.execute(new PostAnnouncementInputData("coach1", " team1 ", "  Bring water.  "));

        assertNull(presenter.error);
        assertEquals(1, store.posts.size());
        final Announcement saved = store.posts.get(0);
        assertEquals("team1", saved.getTeamId());
        assertEquals("coach1", saved.getAuthorUsername());
        assertEquals("Bring water.", saved.getMessage());
        assertEquals(LocalDateTime.of(2026, 11, 9, 18, 30), saved.getCreatedAt());
        assertEquals(saved.getAnnouncementId(), presenter.success.getAnnouncementId());
        assertEquals("team1", presenter.success.getTeamId());
    }

    @Test
    void postingAgainCreatesADifferentAnnouncement() {
        interactor.execute(new PostAnnouncementInputData("coach1", "team1", "First post"));
        interactor.execute(new PostAnnouncementInputData("coach1", "team1", "Second post"));

        assertEquals(2, store.posts.size());
        assertNotEquals(store.posts.get(0).getAnnouncementId(), store.posts.get(1).getAnnouncementId());
    }

    @Test
    void theStoredAuthorIdentityIsPreserved() {
        store.users.put(" coach1 ", new User(" coach1 ", "pw", "Coach", Role.COACH));
        store.teams.put("team1", new Team("team1", "Raptors", "Basketball", " coach1 "));

        interactor.execute(new PostAnnouncementInputData(" coach1 ", "team1", "Team update"));

        assertNull(presenter.error);
        assertEquals(" coach1 ", store.posts.get(0).getAuthorUsername());
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void missingInputsFailBeforeTheStoreIsQueried(PostAnnouncementInputData input) {
        interactor.execute(input);

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertEquals(0, store.accountReads);
        assertTrue(store.posts.isEmpty());
    }

    private static Stream<PostAnnouncementInputData> invalidRequests() {
        return Stream.of(null,
                new PostAnnouncementInputData(null, "team1", "Message"),
                new PostAnnouncementInputData(" \t", "team1", "Message"),
                new PostAnnouncementInputData("coach1", null, "Message"),
                new PostAnnouncementInputData("coach1", "  ", "Message"),
                new PostAnnouncementInputData("coach1", "team1", null),
                new PostAnnouncementInputData("coach1", "team1", " \n"));
    }

    @Test
    void anUnknownAccountCannotPost() {
        interactor.execute(new PostAnnouncementInputData("unknown", "team1", "Message"));

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertTrue(store.posts.isEmpty());
    }

    @Test
    void nonCoachRolesCannotPostEvenIfTheirUsernameIsAssignedToTheTeam() {
        for (Role role : List.of(Role.ADMIN, Role.PLAYER, Role.PARENT)) {
            store.users.put("coach1", new User("coach1", "pw", "Former Coach", role));
            final RecordingPresenter denied = new RecordingPresenter();

            new PostAnnouncementInteractor(store, denied, CLOCK)
                    .execute(new PostAnnouncementInputData("coach1", "team1", "Message"));

            assertNotNull(denied.error, role.toString());
            assertNull(denied.success, role.toString());
        }
        assertTrue(store.posts.isEmpty());
    }

    @Test
    void anotherCoachCannotPostToTheTeam() {
        store.users.put("other", new User("other", "pw", "Other Coach", Role.COACH));

        interactor.execute(new PostAnnouncementInputData("other", "team1", "Message"));

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertTrue(store.posts.isEmpty());
    }

    @Test
    void anUnknownTeamFailsWithoutSaving() {
        interactor.execute(new PostAnnouncementInputData("coach1", "missing", "Message"));

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertTrue(store.posts.isEmpty());
    }

    @Test
    void theCurrentStoredRoleIsCheckedForEveryPost() {
        interactor.execute(new PostAnnouncementInputData("coach1", "team1", "Before role change"));
        store.users.put("coach1", new User("coach1", "pw", "Player Now", Role.PLAYER));
        presenter.success = null;

        interactor.execute(new PostAnnouncementInputData("coach1", "team1", "After role change"));

        assertNotNull(presenter.error);
        assertNull(presenter.success);
        assertEquals(1, store.posts.size());
    }

    @Test
    void anAccountReadFailureIsPresentedWithoutSuccess() {
        store.readFailure = new IllegalStateException("invalid store");

        interactor.execute(new PostAnnouncementInputData("coach1", "team1", "Message"));

        assertTrue(presenter.error.startsWith("Could not save the announcement:"));
        assertNull(presenter.success);
        assertTrue(store.posts.isEmpty());
    }

    @Test
    void aFailedWriteNeverReportsSuccess() {
        for (RuntimeException failure : List.of(new IllegalStateException("read-only store"),
                new UncheckedIOException(new IOException("disk full")))) {
            store.writeFailure = failure;
            final RecordingPresenter failed = new RecordingPresenter();

            new PostAnnouncementInteractor(store, failed, CLOCK)
                    .execute(new PostAnnouncementInputData("coach1", "team1", "Message"));

            assertTrue(failed.error.startsWith("Could not save the announcement:"));
            assertNull(failed.success);
        }
        assertTrue(store.posts.isEmpty());
    }

    /** Saves only after a successful write and records account lookups. */
    private static class FakeStore implements PostAnnouncementDataAccessInterface {
        private final Map<String, User> users = new HashMap<>();
        private final Map<String, Team> teams = new HashMap<>();
        private final List<Announcement> posts = new ArrayList<>();
        private int accountReads;
        private RuntimeException readFailure;
        private RuntimeException writeFailure;

        @Override
        public User get(String username) {
            accountReads++;
            if (readFailure != null) {
                throw readFailure;
            }
            return users.get(username);
        }

        @Override
        public Team getTeam(String teamId) {
            return teams.get(teamId);
        }

        @Override
        public void saveAnnouncement(Announcement announcement) {
            if (writeFailure != null) {
                throw writeFailure;
            }
            posts.add(announcement);
        }
    }

    /** Records whether the use case presented success or failure. */
    private static class RecordingPresenter implements PostAnnouncementOutputBoundary {
        private PostAnnouncementOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(PostAnnouncementOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
