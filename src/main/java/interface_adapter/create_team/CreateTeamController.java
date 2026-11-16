package interface_adapter.create_team;

import interface_adapter.Session;
import use_case.create_team.CreateTeamInputBoundary;
import use_case.create_team.CreateTeamInputData;

/** Submits a new team using the signed-in identity. */
public class CreateTeamController {
    private final CreateTeamInputBoundary interactor;
    private final Session session;

    public CreateTeamController(CreateTeamInputBoundary interactor, Session session) {
        this.interactor = interactor;
        this.session = session;
    }

    /** Attempts to create a team with the entered details. */
    public void execute(String teamId, String name, String sport) {
        final String username = session.isLoggedIn() ? session.getUsername() : "";
        interactor.execute(new CreateTeamInputData(username, teamId, name, sport));
    }
}
