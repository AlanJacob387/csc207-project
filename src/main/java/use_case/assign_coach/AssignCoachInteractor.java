package use_case.assign_coach;

import java.io.UncheckedIOException;

import entity.Role;
import entity.Team;
import entity.User;

/** Lets an admin make a coach account the coach of a team, replacing any earlier coach. */
public class AssignCoachInteractor implements AssignCoachInputBoundary {
    private final AssignCoachDataAccessInterface dataAccess;
    private final AssignCoachOutputBoundary presenter;

    public AssignCoachInteractor(AssignCoachDataAccessInterface dataAccess,
                                 AssignCoachOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(AssignCoachInputData inputData) {
        if (isBlank(inputData.getTeamId()) || isBlank(inputData.getCoachUsername())) {
            presenter.prepareFailView("Enter a team ID and a coach username.");
            return;
        }

        final User user = dataAccess.get(inputData.getUsername());
        if (user == null || user.getRole() != Role.ADMIN) {
            presenter.prepareFailView("Only an admin can assign coaches.");
            return;
        }

        final Team team = dataAccess.getTeam(inputData.getTeamId().trim());
        if (team == null) {
            presenter.prepareFailView("That team does not exist.");
            return;
        }

        final User coach = dataAccess.get(inputData.getCoachUsername().trim());
        if (coach == null || coach.getRole() != Role.COACH) {
            presenter.prepareFailView("That user is not a coach.");
            return;
        }

        team.setCoachUsername(coach.getUsername());
        try {
            dataAccess.saveTeam(team);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not save the team: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(new AssignCoachOutputData(team.getTeamId(), coach.getUsername()));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
