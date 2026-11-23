package use_case.manage_roster;

/** The signed-in user, the team, the player, and whether to add or remove them. */
public final class ManageRosterInputData {
    private final String username;
    private final String teamId;
    private final String playerUsername;
    private final boolean addPlayer;

    /**
     * @param addPlayer true to add the player to the roster, false to remove them
     */
    public ManageRosterInputData(String username, String teamId, String playerUsername,
                                 boolean addPlayer) {
        this.username = username;
        this.teamId = teamId;
        this.playerUsername = playerUsername;
        this.addPlayer = addPlayer;
    }

    public String getUsername() {
        return username;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public boolean isAddPlayer() {
        return addPlayer;
    }
}
