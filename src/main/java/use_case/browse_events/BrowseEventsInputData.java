package use_case.browse_events;

/**
 * Input data for the Browse Events use case: the filters chosen on the list
 * page. A null filter means "don't filter on this".
 */
public class BrowseEventsInputData {
    private final String type;      // "PRACTICE", "GAME" or null for both
    private final String teamId;    // one of the user's teams, or null for all

    /**
     * Creates the input data.
     *
     * @param type   "PRACTICE", "GAME", or null for every type
     * @param teamId a team to narrow to, or null for all of the user's teams
     */
    public BrowseEventsInputData(String type, String teamId) {
        this.type = type;
        this.teamId = teamId;
    }

    public String getType() {
        return type;
    }

    public String getTeamId() {
        return teamId;
    }
}
