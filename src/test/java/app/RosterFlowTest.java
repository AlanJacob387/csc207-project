package app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.User;
import interface_adapter.Session;
import interface_adapter.assign_coach.AssignCoachController;
import interface_adapter.assign_coach.AssignCoachPresenter;
import interface_adapter.assign_coach.AssignCoachViewModel;
import interface_adapter.create_team.CreateTeamController;
import interface_adapter.create_team.CreateTeamPresenter;
import interface_adapter.create_team.CreateTeamViewModel;
import interface_adapter.manage_roster.ManageRosterController;
import interface_adapter.manage_roster.ManageRosterPresenter;
import interface_adapter.manage_roster.ManageRosterViewModel;
import interface_adapter.view_roster.ViewRosterController;
import interface_adapter.view_roster.ViewRosterPresenter;
import interface_adapter.view_roster.ViewRosterViewModel;
import use_case.assign_coach.AssignCoachInteractor;
import use_case.create_team.CreateTeamInteractor;
import use_case.manage_roster.ManageRosterInteractor;
import use_case.view_roster.ViewRosterInteractor;

/** Runs the roster slices against the real JSON store in a temporary folder. */
class RosterFlowTest {

    @TempDir
    Path tempDir;

    @Test
    void adminBuildsATeamAndTheCoachSeesItAfterAReload() {
        final Path file = tempDir.resolve("app-data.json");
        final FileAppDataAccessObject store = new FileAppDataAccessObject(file);
        store.save(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        store.save(new User("coach1", "pw", "Coach One", Role.COACH));
        store.save(new User("player1", "pw", "Pat Player", Role.PLAYER));

        final Session session = new Session();
        session.logIn("admin1", "Ada Admin", Role.ADMIN);

        final CreateTeamViewModel createViewModel = new CreateTeamViewModel();
        new CreateTeamController(new CreateTeamInteractor(store,
                new CreateTeamPresenter(createViewModel)), session)
                .execute("u12", "Raptors", "Basketball");
        final AssignCoachViewModel coachViewModel = new AssignCoachViewModel();
        new AssignCoachController(new AssignCoachInteractor(store,
                new AssignCoachPresenter(coachViewModel)), session)
                .execute("u12", "coach1");
        final ManageRosterViewModel rosterViewModel = new ManageRosterViewModel();
        new ManageRosterController(new ManageRosterInteractor(store,
                new ManageRosterPresenter(rosterViewModel)), session)
                .addPlayer("u12", "player1");

        assertEquals("Created Raptors (u12).", createViewModel.getSuccessMessage());
        assertEquals("coach1 now coaches u12.", coachViewModel.getSuccessMessage());
        assertEquals("player1 added to u12.", rosterViewModel.getSuccessMessage());

        // A fresh store reads the file again, as the app does after a restart.
        final FileAppDataAccessObject reloaded = new FileAppDataAccessObject(file);
        session.logIn("coach1", "Coach One", Role.COACH);
        final ViewRosterViewModel viewModel = new ViewRosterViewModel();
        new ViewRosterController(new ViewRosterInteractor(reloaded,
                new ViewRosterPresenter(viewModel)), session).execute();

        assertEquals("", viewModel.getErrorMessage());
        assertEquals("Raptors (u12) - Basketball\n    Coach: coach1\n    Players: player1",
                viewModel.getRosterText());
    }
}
