package use_case.parent_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.AbsenceNotice;
import entity.Event;
import entity.ParentPlayerLink;
import entity.Role;
import entity.Team;
import entity.User;
import interface_adapter.Session;
import use_case.link_parent_to_player.*;
import use_case.submit_absence_notice.*;
import use_case.view_absence_notices.*;

/** Exercises the parent link, absence form, and coach feed using one isolated fake. */
class ParentAccessInteractorTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
    private Store store;
    private Session session;
    private Recording result;

    @BeforeEach
    void setUp() {
        store = new Store();
        addUser("admin", Role.ADMIN);
        addUser("parent", Role.PARENT);
        addUser("other-parent", Role.PARENT);
        addUser("child", Role.PLAYER);
        addUser("other-child", Role.PLAYER);
        addUser("coach", Role.COACH);
        addUser("other-coach", Role.COACH);
        final Team team = new Team("team", "Raptors", "Basketball", "coach");
        team.addPlayer("child");
        store.teams.put("team", team);
        final Team other = new Team("other-team", "Bears", "Soccer", "other-coach");
        other.addPlayer("other-child");
        store.teams.put("other-team", other);
        store.events.put("event", event("event", "team", NOW.plusDays(1)));
        store.events.put("other-event", event("other-event", "other-team", NOW.plusDays(1)));
        store.links.add(new ParentPlayerLink("parent", "child"));
        session = new Session();
        signIn("parent");
        result = new Recording();
    }

    @Test
    void aLinkedParentCanSaveAndUpdateOneNoticeWithoutDuplicates() {
        submit("child", "event", "  Doctor appointment  ");

        assertNull(result.error);
        assertNotNull(result.submitted);
        final AbsenceNotice first = store.notices.values().iterator().next();
        assertEquals("parent", first.getParentUsername());
        assertEquals("Doctor appointment", first.getReason());
        assertEquals(NOW, first.getSubmittedAt());

        submit("child", "event", "Appointment moved");

        assertEquals(1, store.notices.size());
        assertEquals(first.getNoticeId(), result.submitted.noticeId());
        assertEquals("Appointment moved", store.notices.get(first.getNoticeId()).getReason());
    }

    @Test
    void aParentCannotSubmitForAnUnlinkedPlayer() {
        submit("other-child", "other-event", "Away");

        assertFailedSubmit();
    }

    @Test
    void aLinkedPlayerMustBelongToTheEventsTeam() {
        submit("child", "other-event", "Away");

        assertFailedSubmit();
    }

    @Test
    void onlyParentsCanSubmitEvenWhenALinkExists() {
        signIn("coach");
        store.links.add(new ParentPlayerLink("coach", "child"));

        submit("child", "event", "Away");

        assertFailedSubmit();
    }

    @Test
    void anUnknownOrSignedOutParentCannotSubmit() {
        session.clearCurrentUser();
        submit("child", "event", "Away");
        assertFailedSubmit();

        session.setCurrentUser(new User("missing", "pw", "Missing", Role.PARENT));
        submit("child", "event", "Away");
        assertFailedSubmit();
    }

    @Test
    void cancelledPastAndUnknownEventsAreRejected() {
        store.events.get("event").cancel();
        submit("child", "event", "Away");
        assertFailedSubmit();

        store.events.put("event", event("event", "team", NOW));
        submit("child", "event", "Away");
        assertFailedSubmit();

        submit("child", "missing", "Away");
        assertFailedSubmit();
    }

    @Test
    void aReasonAndSelectionsAreRequired() {
        submit("child", "event", " \t");
        assertFailedSubmit();
        submit(null, "event", "Away");
        assertFailedSubmit();
        submit("child", null, "Away");
        assertFailedSubmit();
    }

    @Test
    void storedRoleAndLinkChangesAreRecheckedForSubmission() {
        store.users.put("parent", new User("parent", "pw", "Parent", Role.PLAYER));
        submit("child", "event", "Away");
        assertFailedSubmit();

        addUser("parent", Role.PARENT);
        store.links.clear();
        submit("child", "event", "Away");
        assertFailedSubmit();
    }

    @Test
    void aFailedAbsenceWriteReportsFailureWithoutSuccess() {
        store.failWrites = true;

        submit("child", "event", "Away");

        assertFailedSubmit();
        assertTrue(result.error.contains("Could not save"));
    }

    @Test
    void aParentSeesOnlyLinkedChoicesAndTheirOwnNoticeForTheSelectedChild() {
        store.events.put("past", event("past", "team", NOW.minusDays(1)));
        final Event cancelled = event("cancelled", "team", NOW.plusDays(1));
        cancelled.cancel();
        store.events.put("cancelled", cancelled);
        store.notices.put("mine", notice("mine", "parent", "child", "event"));
        store.notices.put("someone-else", notice("someone-else", "other-parent", "child", "event"));

        view(null);

        assertNull(result.error);
        assertEquals("child", result.viewed.selectedPlayerUsername());
        assertEquals(List.of("child"), result.viewed.children().stream().map(row -> row.username()).toList());
        assertEquals(List.of("event"), result.viewed.events().stream().map(row -> row.eventId()).toList());
        assertEquals(List.of("mine"), result.viewed.notices().stream().map(row -> row.noticeId()).toList());
        assertTrue(result.viewed.canSubmit());
    }

    @Test
    void anUnlinkedSelectionIsDeniedInsteadOfFallingBackToAnotherChild() {
        view("other-child");

        assertNotNull(result.error);
        assertNull(result.viewed);
    }

    @Test
    void aCoachSeesAllNoticesForOnlyTheirOwnTeams() {
        store.notices.put("one", notice("one", "parent", "child", "event"));
        store.notices.put("two", notice("two", "other-parent", "other-child", "other-event"));
        signIn("coach");

        view(null);

        assertNull(result.error);
        assertEquals(List.of("one"), result.viewed.notices().stream().map(row -> row.noticeId()).toList());
        assertEquals("child", result.viewed.notices().get(0).playerUsername());
        assertEquals("parent", result.viewed.notices().get(0).parentUsername());
        assertTrue(result.viewed.children().isEmpty());
        assertFalse(result.viewed.canSubmit());
    }

    @Test
    void anUnlinkedParentGetsAnEmptyStateWithNoOldChildData() {
        store.links.clear();
        store.notices.put("old", notice("old", "parent", "child", "event"));

        view(null);

        assertNull(result.error);
        assertTrue(result.viewed.children().isEmpty());
        assertTrue(result.viewed.events().isEmpty());
        assertTrue(result.viewed.notices().isEmpty());
    }

    @Test
    void aPlayerCannotReadAbsenceReasons() {
        signIn("child");

        view(null);

        assertNotNull(result.error);
        assertNull(result.viewed);
    }

    @Test
    void anAdminCanLinkUnlinkAndListEligibleAccounts() {
        signIn("admin");

        link("link", "other-parent", "other-child");

        assertNull(result.error);
        assertEquals(2, result.linked.parents().size());
        assertEquals(2, result.linked.players().size());
        assertEquals(2, result.linked.links().size());

        link("link", "other-parent", "other-child");
        assertEquals(2, store.links.size());

        link("unlink", "other-parent", "other-child");
        assertEquals(1, store.links.size());
    }

    @Test
    void aParentCannotApproveTheirOwnLinkOrListOtherAccounts() {
        link("link", "parent", "other-child");

        assertNotNull(result.error);
        assertNull(result.linked);
        assertEquals(1, store.links.size());
    }

    @Test
    void linksRequireAnExistingParentAndPlayerWithTheExpectedRoles() {
        signIn("admin");
        link("link", "coach", "child");
        assertNotNull(result.error);
        assertNull(result.linked);
        link("link", "parent", "missing");
        assertNotNull(result.error);
        assertNull(result.linked);
        link("link", "parent", "other-parent");
        assertNotNull(result.error);
        assertEquals(1, store.links.size());
    }

    @Test
    void anAdminCanRemoveAStaleLinkAfterAnAccountsRoleChanges() {
        signIn("admin");
        addUser("parent", Role.COACH);

        link("unlink", "parent", "child");

        assertNull(result.error);
        assertTrue(store.links.isEmpty());
    }

    @Test
    void failedLinkWritesReportFailureWithoutClaimingSuccess() {
        signIn("admin");
        store.failWrites = true;

        link("link", "other-parent", "other-child");

        assertNotNull(result.error);
        assertNull(result.linked);
        assertEquals(1, store.links.size());
    }

    @Test
    void readErrorsDoNotBecomeEmptySuccessfulPages() {
        store.failReads = true;

        view(null);

        assertNotNull(result.error);
        assertNull(result.viewed);
        signIn("admin");
        link("load", null, null);
        assertNotNull(result.error);
        assertNull(result.linked);
    }

    private void addUser(String username, Role role) {
        store.users.put(username, new User(username, "pw", username, role));
    }

    private void signIn(String username) {
        session.setCurrentUser(store.users.get(username));
    }

    private void submit(String child, String eventId, String reason) {
        result = new Recording();
        new SubmitAbsenceNoticeInteractor(store, result, session, CLOCK)
                .execute(new SubmitAbsenceNoticeInputData(child, eventId, reason));
    }

    private void view(String child) {
        result = new Recording();
        new ViewAbsenceNoticesInteractor(store, result, session, CLOCK)
                .execute(new ViewAbsenceNoticesInputData(child));
    }

    private void link(String action, String parent, String child) {
        result = new Recording();
        new LinkParentToPlayerInteractor(store, result, session)
                .execute(new LinkParentToPlayerInputData(action, parent, child));
    }

    private void assertFailedSubmit() {
        assertNotNull(result.error);
        assertNull(result.submitted);
        assertTrue(store.notices.isEmpty());
    }

    private static Event event(String id, String team, LocalDateTime time) {
        return new Event(id, team, Event.EventType.PRACTICE, time, "Gym", null);
    }

    private static AbsenceNotice notice(String id, String parent, String child, String event) {
        return new AbsenceNotice(id, parent, child, event, "Away", NOW);
    }

    private static class Recording implements SubmitAbsenceNoticeOutputBoundary,
            ViewAbsenceNoticesOutputBoundary, LinkParentToPlayerOutputBoundary {
        private SubmitAbsenceNoticeOutputData submitted;
        private ViewAbsenceNoticesOutputData viewed;
        private LinkParentToPlayerOutputData linked;
        private String error;

        @Override
        public void prepareSuccessView(SubmitAbsenceNoticeOutputData data) { submitted = data; }
        @Override
        public void prepareSuccessView(ViewAbsenceNoticesOutputData data) { viewed = data; }
        @Override
        public void prepareSuccessView(LinkParentToPlayerOutputData data) { linked = data; }
        @Override
        public void prepareFailView(String message) { error = message; }
    }

    private static class Store implements SubmitAbsenceNoticeDataAccessInterface,
            ViewAbsenceNoticesDataAccessInterface, LinkParentToPlayerDataAccessInterface {
        private final Map<String, User> users = new LinkedHashMap<>();
        private final Map<String, Team> teams = new LinkedHashMap<>();
        private final Map<String, Event> events = new LinkedHashMap<>();
        private final List<ParentPlayerLink> links = new ArrayList<>();
        private final Map<String, AbsenceNotice> notices = new LinkedHashMap<>();
        private boolean failWrites;
        private boolean failReads;

        @Override
        public User get(String username) {
            if (failReads) { throw new IllegalStateException("Cannot read data"); }
            return users.get(username);
        }
        @Override
        public Team getTeam(String id) { return teams.get(id); }
        @Override
        public Event getEvent(String id) { return events.get(id); }
        @Override
        public List<User> getAllUsers() { return new ArrayList<>(users.values()); }
        @Override
        public List<Team> getAllTeams() { return new ArrayList<>(teams.values()); }
        @Override
        public List<Event> getAllEvents() { return new ArrayList<>(events.values()); }
        @Override
        public List<ParentPlayerLink> getParentPlayerLinks() { return new ArrayList<>(links); }
        @Override
        public List<AbsenceNotice> getAbsenceNotices() { return new ArrayList<>(notices.values()); }
        @Override
        public List<String> getLinkedPlayerUsernames(String parent) {
            return links.stream().filter(link -> link.getParentUsername().equals(parent))
                    .map(ParentPlayerLink::getPlayerUsername).toList();
        }
        @Override
        public void saveParentPlayerLink(ParentPlayerLink link) {
            checkWrite();
            deleteParentPlayerLink(link.getParentUsername(), link.getPlayerUsername());
            links.add(link);
        }
        @Override
        public void deleteParentPlayerLink(String parent, String player) {
            checkWrite();
            links.removeIf(link -> link.getParentUsername().equals(parent) && link.getPlayerUsername().equals(player));
        }
        @Override
        public void saveAbsenceNotice(AbsenceNotice notice) {
            checkWrite();
            notices.put(notice.getNoticeId(), notice);
        }
        private void checkWrite() {
            if (failWrites) { throw new UncheckedIOException(new IOException("disk full")); }
        }
    }
}
