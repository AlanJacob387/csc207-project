package use_case.create_team;

/** Identifies the team that was created. */
public final class CreateTeamOutputData {
    private final String teamId;
    private final String name;

    public CreateTeamOutputData(String teamId, String name) {
        this.teamId = teamId;
        this.name = name;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getName() {
        return name;
    }
}
