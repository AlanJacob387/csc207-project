package use_case.manage_roster;

import java.io.UncheckedIOException;

import entity.Role;
import entity.Team;
import entity.User;

/** Lets an admin add a player account to a team or remove a player from it. */
public class ManageRosterInteractor implements ManageRosterInputBoundary {
    private final ManageRosterDataAccessInterface dataAccess;
    private final ManageRosterOutputBoundary presenter;

    public ManageRosterInteractor(ManageRosterDataAccessInterface dataAccess,
                                  ManageRosterOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(ManageRosterInputData inputData) {
        if (isBlank(inputData.getTeamId()) || isBlank(inputData.getPlayerUsername())) {
            presenter.prepareFailView("Enter a team ID and a player username.");
            return;
        }

        final User user = dataAccess.get(inputData.getUsername());
        if (user == null || user.getRole() != Role.ADMIN) {
            presenter.prepareFailView("Only an admin can change a roster.");
            return;
        }

        final Team team = dataAccess.getTeam(inputData.getTeamId().trim());
        if (team == null) {
            presenter.prepareFailView("That team does not exist.");
            return;
        }

        final String playerUsername = inputData.getPlayerUsername().trim();
        final boolean onRoster = team.getPlayerUsernames().contains(playerUsername);

        if (inputData.isAddPlayer()) {
            final User player = dataAccess.get(playerUsername);
            if (player == null || player.getRole() != Role.PLAYER) {
                presenter.prepareFailView("That user is not a player.");
                return;
            }
            if (onRoster) {
                presenter.prepareFailView("That player is already on the team.");
                return;
            }
            team.addPlayer(playerUsername);
        }
        else {
            if (!onRoster) {
                presenter.prepareFailView("That player is not on the team.");
                return;
            }
            team.removePlayer(playerUsername);
        }

        try {
            dataAccess.saveTeam(team);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the team: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(new ManageRosterOutputData(
                team.getTeamId(), playerUsername, inputData.isAddPlayer()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
