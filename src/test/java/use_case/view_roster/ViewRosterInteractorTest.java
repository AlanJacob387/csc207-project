package use_case.view_roster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Role;
import entity.Team;
import entity.User;
import use_case.view_roster.ViewRosterOutputData.Person;
import use_case.view_roster.ViewRosterOutputData.TeamRoster;

/** Checks which teams and accounts each role can see. */
class ViewRosterInteractorTest {
    private FakeStore store;
    private RecordingPresenter presenter;
    private ViewRosterInteractor interactor;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        store.add(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.add(new User("coach1", "pw", "Coach One", Role.COACH));
        store.add(new User("coach3", "pw", "Coach Three", Role.COACH));
        store.add(new User("player1", "pw", "Pat Player", Role.PLAYER));
        store.add(new User("player2", "pw", "Max Player", Role.PLAYER));
        final Team raptors = new Team("t1", "Raptors", "Basketball", "coach1");
        raptors.addPlayer("player1");
        store.teams.add(raptors);
        store.teams.add(new Team("t2", "Lions", "Soccer", ""));
        presenter = new RecordingPresenter();
        interactor = new ViewRosterInteractor(store, presenter);
    }

    @Test
    void adminSeesEveryTeamWithNamesFilledIn() {
        interactor.execute(new ViewRosterInputData("admin1"));

        assertNull(presenter.error);
        final List<TeamRoster> teams = presenter.success.getTeams();
        assertEquals(2, teams.size());
        final TeamRoster raptors = teams.get(0);
        assertEquals("t1", raptors.getTeamId());
        assertEquals("Raptors", raptors.getName());
        assertEquals("Basketball", raptors.getSport());
        assertEquals("coach1", raptors.getCoach().getUsername());
        assertEquals("Coach One", raptors.getCoach().getDisplayName());
        assertEquals(1, raptors.getPlayers().size());
        assertEquals("Pat Player", raptors.getPlayers().get(0).getDisplayName());
        assertNull(teams.get(1).getCoach());
    }

    @Test
    void adminGetsTheCoachAndPlayerAccountsToChooseFrom() {
        interactor.execute(new ViewRosterInputData("admin1"));

        assertEquals(List.of("coach1", "coach3"), usernames(presenter.success.getCoaches()));
        assertEquals(List.of("player1", "player2"), usernames(presenter.success.getPlayers()));
    }

    @Test
    void coachSeesOnlyTheirTeamsAndNoAccountLists() {
        interactor.execute(new ViewRosterInputData("coach1"));

        assertNull(presenter.error);
        assertEquals(1, presenter.success.getTeams().size());
        assertEquals("t1", presenter.success.getTeams().get(0).getTeamId());
        assertTrue(presenter.success.getCoaches().isEmpty());
        assertTrue(presenter.success.getPlayers().isEmpty());
    }

    @Test
    void coachWithNoTeamsSeesAnEmptyList() {
        interactor.execute(new ViewRosterInputData("coach3"));

        assertNull(presenter.error);
        assertTrue(presenter.success.getTeams().isEmpty());
    }

    @Test
    void playerWhoseAccountWasDeletedStillShowsByUsername() {
        store.teams.get(0).addPlayer("gone");

        interactor.execute(new ViewRosterInputData("admin1"));

        final Person gone = presenter.success.getTeams().get(0).getPlayers().get(1);
        assertEquals("gone", gone.getUsername());
        assertEquals("gone", gone.getDisplayName());
    }

    @Test
    void playerIsRejected() {
        interactor.execute(new ViewRosterInputData("player1"));

        assertNull(presenter.success);
        assertEquals("Only admins and coaches can view rosters.", presenter.error);
    }

    @Test
    void unknownUserIsRejected() {
        interactor.execute(new ViewRosterInputData("ghost"));

        assertNull(presenter.success);
        assertEquals("Only admins and coaches can view rosters.", presenter.error);
    }

    private static List<String> usernames(List<Person> people) {
        final List<String> names = new ArrayList<>();
        for (Person person : people) {
            names.add(person.getUsername());
        }
        return names;
    }

    private static final class FakeStore implements ViewRosterDataAccessInterface {
        private final Map<String, User> users = new LinkedHashMap<>();
        private final List<Team> teams = new ArrayList<>();

        void add(User user) {
            users.put(user.getUsername(), user);
        }

        @Override
        public User get(String username) {
            return users.get(username);
        }

        @Override
        public List<Team> getAllTeams() {
            return new ArrayList<>(teams);
        }

        @Override
        public List<User> getAllUsers() {
            return new ArrayList<>(users.values());
        }
    }

    private static final class RecordingPresenter implements ViewRosterOutputBoundary {
        private ViewRosterOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ViewRosterOutputData outputData) {
            success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            error = errorMessage;
        }
    }
}
