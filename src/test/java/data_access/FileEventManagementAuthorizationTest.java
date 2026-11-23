package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import entity.Event;
import entity.ParentPlayerLink;
import entity.Role;
import entity.Team;
import entity.User;
import interface_adapter.Session;
import use_case.cancel_event.CancelEventInputData;
import use_case.cancel_event.CancelEventInteractor;
import use_case.cancel_event.CancelEventOutputBoundary;
import use_case.cancel_event.CancelEventOutputData;
import use_case.create_event.CreateEventInputData;
import use_case.create_event.CreateEventInteractor;
import use_case.create_event.CreateEventOutputBoundary;
import use_case.create_event.CreateEventOutputData;
import use_case.edit_event.EditEventInputData;
import use_case.edit_event.EditEventInteractor;
import use_case.edit_event.EditEventOutputBoundary;
import use_case.edit_event.EditEventOutputData;
import use_case.view_event_details.ViewEventDetailsInputData;
import use_case.view_event_details.ViewEventDetailsInteractor;
import use_case.view_event_details.ViewEventDetailsOutputBoundary;
import use_case.view_event_details.ViewEventDetailsOutputData;

/** Verifies role changes against the real store, not just a matching coach username. */
class FileEventManagementAuthorizationTest {
    @TempDir
    Path directory;
    private FileAppDataAccessObject store;
    private Session session;
    private LocalDateTime future;

    @BeforeEach
    void setUp() {
        store = new FileAppDataAccessObject(file());
        final User coach = new User("assigned-coach", "pw", "Coach", Role.COACH);
        store.save(coach);
        store.save(new User("child", "pw", "Child", Role.PLAYER));
        final Team team = new Team("team", "Team", "Soccer", "assigned-coach");
        team.addPlayer("child");
        store.saveTeam(team);
        future = LocalDateTime.now().plusDays(2).withNano(0);
        store.saveEvent(new Event("existing", "team", Event.EventType.GAME, future, "Original field", "Rivals"));
        session = new Session();
        session.setCurrentUser(coach);
    }

    @Test
    void aDemotedCoachWithALinkedChildCanReadButCannotCreateEditOrCancelEvents() throws IOException {
        store.save(new User("assigned-coach", "pw", "Parent now", Role.PARENT));
        store.saveParentPlayerLink(new ParentPlayerLink("assigned-coach", "child"));
        // The old session still says COACH; authorization must read the current account.
        assertEquals(Role.COACH, session.getRole());
        assertTrue(store.canViewEvent("assigned-coach", "team"));
        assertFalse(store.canManageEvent("assigned-coach", "team"));
        final String before = Files.readString(file());
        final Outcomes output = new Outcomes();

        new ViewEventDetailsInteractor(store, output, session).execute(new ViewEventDetailsInputData("existing"));
        assertFalse(output.details.isCanManage());
        new CreateEventInteractor(store, output, session).execute(
                new CreateEventInputData("team", "GAME", future, "New field", "Rivals"));
        new EditEventInteractor(store, output, session).execute(
                new EditEventInputData("existing", future, "New field", "Rivals"));
        new CancelEventInteractor(store, output, session).execute(new CancelEventInputData("existing"));

        assertEquals(0, output.mutations);
        assertEquals(3, output.errors.size());
        assertEquals(1, store.getAllEvents().size());
        assertEquals("Original field", store.getEvent("existing").getLocation());
        assertFalse(store.getEvent("existing").isCancelled());
        assertEquals(1, store.getEventNotifications().size());
        assertEquals(before, Files.readString(file()));
        assertFalse(new FileAppDataAccessObject(file()).getEvent("existing").isCancelled());
    }

    @Test
    void aStoredCoachRoleAndExactTeamAssignmentAreBothRequired() {
        for (Role role : List.of(Role.ADMIN, Role.PLAYER, Role.PARENT)) {
            store.save(new User("assigned-coach", "pw", "Former coach", role));
            assertFalse(store.canManageEvent("assigned-coach", "team"), role.name());
        }
        store.save(new User("assigned-coach", "pw", "Coach", Role.COACH));
        store.save(new User("other-coach", "pw", "Other Coach", Role.COACH));

        assertTrue(store.canManageEvent("assigned-coach", "team"));
        assertFalse(store.canManageEvent("other-coach", "team"));
        assertFalse(store.canManageEvent("missing-account", "team"));
        assertFalse(store.canManageEvent("assigned-coach", "missing-team"));
    }

    @Test
    void theCurrentAssignedCoachRetainsTheCompleteEventLifecycle() {
        final Outcomes output = new Outcomes();
        new ViewEventDetailsInteractor(store, output, session).execute(new ViewEventDetailsInputData("existing"));
        assertTrue(output.details.isCanManage());

        new EditEventInteractor(store, output, session).execute(
                new EditEventInputData("existing", future, "New field", "Rivals"));
        new CreateEventInteractor(store, output, session).execute(
                new CreateEventInputData("team", "PRACTICE", future, "New field", null));
        new CancelEventInteractor(store, output, session).execute(new CancelEventInputData("existing"));

        assertTrue(output.errors.isEmpty());
        assertEquals(3, output.mutations);
        assertEquals(2, store.getAllEvents().size());
        assertEquals("New field", store.getEvent("existing").getLocation());
        assertTrue(store.getEvent("existing").isCancelled());
        assertEquals(4, store.getEventNotifications().size());
    }

    private Path file() {
        return directory.resolve("app.json");
    }

    private static class Outcomes implements CreateEventOutputBoundary, EditEventOutputBoundary,
            CancelEventOutputBoundary, ViewEventDetailsOutputBoundary {
        private final List<String> errors = new ArrayList<>();
        private ViewEventDetailsOutputData details;
        private int mutations;

        @Override
        public void prepareSuccessView(CreateEventOutputData output) {
            mutations++;
        }

        @Override
        public void prepareSuccessView(EditEventOutputData output) {
            mutations++;
        }

        @Override
        public void prepareSuccessView(CancelEventOutputData output) {
            mutations++;
        }

        @Override
        public void prepareSuccessView(ViewEventDetailsOutputData output) {
            details = output;
        }

        @Override
        public void prepareFailView(String message) {
            errors.add(message);
        }
    }
}
