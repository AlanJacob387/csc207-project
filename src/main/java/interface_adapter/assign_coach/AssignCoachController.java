package interface_adapter.assign_coach;

import interface_adapter.Session;
import use_case.assign_coach.AssignCoachInputBoundary;
import use_case.assign_coach.AssignCoachInputData;

/** Submits a coach assignment using the signed-in identity. */
public class AssignCoachController {
    private final AssignCoachInputBoundary interactor;
    private final Session session;

    public AssignCoachController(AssignCoachInputBoundary interactor, Session session) {
        this.interactor = interactor;
        this.session = session;
    }

    /** Attempts to make the given coach the coach of the given team. */
    public void execute(String teamId, String coachUsername) {
        final String username = session.isLoggedIn() ? session.getCurrentUsername() : "";
        interactor.execute(new AssignCoachInputData(username, teamId, coachUsername));
    }
}
