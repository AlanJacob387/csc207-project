package use_case.view_roster;

/** The signed-in user asking to see rosters. */
public final class ViewRosterInputData {
    private final String username;

    public ViewRosterInputData(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
