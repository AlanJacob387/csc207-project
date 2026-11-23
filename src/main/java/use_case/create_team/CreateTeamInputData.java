package use_case.create_team;

/** The signed-in user and the details entered for the new team. */
public final class CreateTeamInputData {
    private final String username;
    private final String teamId;
    private final String name;
    private final String sport;

    public CreateTeamInputData(String username, String teamId, String name, String sport) {
        this.username = username;
        this.teamId = teamId;
        this.name = name;
        this.sport = sport;
    }

    public String getUsername() {
        return username;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getName() {
        return name;
    }

    public String getSport() {
        return sport;
    }
}
