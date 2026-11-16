package use_case.manage_roster;

/** The roster change that was saved. */
public final class ManageRosterOutputData {
    private final String teamId;
    private final String playerUsername;
    private final boolean added;

    public ManageRosterOutputData(String teamId, String playerUsername, boolean added) {
        this.teamId = teamId;
        this.playerUsername = playerUsername;
        this.added = added;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    /** True if the player was added, false if they were removed. */
    public boolean isAdded() {
        return added;
    }
}
