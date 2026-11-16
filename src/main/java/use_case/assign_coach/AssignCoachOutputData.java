package use_case.assign_coach;

/** The team and the coach now assigned to it. */
public final class AssignCoachOutputData {
    private final String teamId;
    private final String coachUsername;

    public AssignCoachOutputData(String teamId, String coachUsername) {
        this.teamId = teamId;
        this.coachUsername = coachUsername;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getCoachUsername() {
        return coachUsername;
    }
}
