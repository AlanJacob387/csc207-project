package use_case.view_roster;

/** Accepts a request to list the teams and rosters the viewer may see. */
public interface ViewRosterInputBoundary {
    void execute(ViewRosterInputData inputData);
}
