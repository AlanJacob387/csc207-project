package use_case.assign_coach;

/** The signed-in user, the team, and the coach to assign to it. */
public final class AssignCoachInputData {
    private final String username;
    private final String teamId;
    private final String coachUsername;

    public AssignCoachInputData(String username, String teamId, String coachUsername) {
        this.username = username;
        this.teamId = teamId;
        this.coachUsername = coachUsername;
    }

    public String getUsername() {
        return username;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getCoachUsername() {
        return coachUsername;
    }
}
