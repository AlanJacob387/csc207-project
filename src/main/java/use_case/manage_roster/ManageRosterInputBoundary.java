package use_case.manage_roster;

/** Accepts a request to add a player to, or remove a player from, a team. */
public interface ManageRosterInputBoundary {
    void execute(ManageRosterInputData inputData);
}
