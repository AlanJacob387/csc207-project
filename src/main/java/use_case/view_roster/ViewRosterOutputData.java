package use_case.view_roster;

import java.util.List;

/** The teams the viewer may see, each with its coach and players. */
public final class ViewRosterOutputData {
    private final List<TeamRoster> teams;

    public ViewRosterOutputData(List<TeamRoster> teams) {
        this.teams = List.copyOf(teams);
    }

    public List<TeamRoster> getTeams() {
        return teams;
    }

    /** One team's details, copied out of the entity so the view never edits it. */
    public static final class TeamRoster {
        private final String teamId;
        private final String name;
        private final String sport;
        private final String coachUsername;
        private final List<String> playerUsernames;

        public TeamRoster(String teamId, String name, String sport, String coachUsername,
                          List<String> playerUsernames) {
            this.teamId = teamId;
            this.name = name;
            this.sport = sport;
            this.coachUsername = coachUsername;
            this.playerUsernames = List.copyOf(playerUsernames);
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

        /** The coach's username, or an empty string if no coach is assigned yet. */
        public String getCoachUsername() {
            return coachUsername;
        }

        public List<String> getPlayerUsernames() {
            return playerUsernames;
        }
    }
}
