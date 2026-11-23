package use_case.create_team;

import java.io.UncheckedIOException;

import entity.Role;
import entity.Team;
import entity.User;

/** Lets an admin create a team with a unique ID. The coach is assigned separately. */
public class CreateTeamInteractor implements CreateTeamInputBoundary {
    private final CreateTeamDataAccessInterface dataAccess;
    private final CreateTeamOutputBoundary presenter;

    public CreateTeamInteractor(CreateTeamDataAccessInterface dataAccess,
                                CreateTeamOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(CreateTeamInputData inputData) {
        if (isBlank(inputData.getTeamId()) || isBlank(inputData.getName())
                || isBlank(inputData.getSport())) {
            presenter.prepareFailView("Enter a team ID, name and sport.");
            return;
        }

        final User user = dataAccess.get(inputData.getUsername());
        if (user == null || user.getRole() != Role.ADMIN) {
            presenter.prepareFailView("Only an admin can create teams.");
            return;
        }

        final String teamId = inputData.getTeamId().trim();
        if (dataAccess.teamExists(teamId)) {
            presenter.prepareFailView("A team with ID \"" + teamId + "\" already exists.");
            return;
        }

        final Team team = new Team(teamId, inputData.getName().trim(),
                inputData.getSport().trim(), "");
        try {
            dataAccess.saveTeam(team);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the team: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(new CreateTeamOutputData(team.getTeamId(), team.getName()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
