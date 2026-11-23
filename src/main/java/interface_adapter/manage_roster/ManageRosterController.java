package interface_adapter.manage_roster;

import interface_adapter.Session;
import use_case.manage_roster.ManageRosterInputBoundary;
import use_case.manage_roster.ManageRosterInputData;

/** Submits roster changes using the signed-in identity. */
public class ManageRosterController {
    private final ManageRosterInputBoundary interactor;
    private final Session session;

    public ManageRosterController(ManageRosterInputBoundary interactor, Session session) {
        this.interactor = interactor;
        this.session = session;
    }

    /** Attempts to add the player to the team. */
    public void addPlayer(String teamId, String playerUsername) {
        interactor.execute(new ManageRosterInputData(currentUser(), teamId, playerUsername, true));
    }

    /** Attempts to remove the player from the team. */
    public void removePlayer(String teamId, String playerUsername) {
        interactor.execute(new ManageRosterInputData(currentUser(), teamId, playerUsername, false));
    }

    private String currentUser() {
        return session.isLoggedIn() ? session.getCurrentUsername() : "";
    }
}
