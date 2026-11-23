package use_case.view_roster;

import java.util.List;

/**
 * The teams the viewer may see, plus the coach and player accounts an admin can
 * choose from. The account lists are empty for coaches, who cannot edit rosters.
 */
public final class ViewRosterOutputData {
    private final List<TeamRoster> teams;
    private final List<Person> coaches;
    private final List<Person> players;

    public ViewRosterOutputData(List<TeamRoster> teams, List<Person> coaches, List<Person> players) {
        this.teams = List.copyOf(teams);
        this.coaches = List.copyOf(coaches);
        this.players = List.copyOf(players);
    }

    public List<TeamRoster> getTeams() {
        return teams;
    }

    /** Every coach account, for the admin's coach dropdown. */
    public List<Person> getCoaches() {
        return coaches;
    }

    /** Every player account, for the admin's add player dropdown. */
    public List<Person> getPlayers() {
        return players;
    }

    /** A user's username and display name. */
    public static final class Person {
        private final String username;
        private final String displayName;

        public Person(String username, String displayName) {
            this.username = username;
            this.displayName = displayName;
        }

        public String getUsername() {
            return username;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /** One team's details, copied out of the entity so the view never edits it. */
    public static final class TeamRoster {
        private final String teamId;
        private final String name;
        private final String sport;
        private final Person coach;
        private final List<Person> players;

        public TeamRoster(String teamId, String name, String sport, Person coach,
                          List<Person> players) {
            this.teamId = teamId;
            this.name = name;
            this.sport = sport;
            this.coach = coach;
            this.players = List.copyOf(players);
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

        /** The team's coach, or null if no coach is assigned yet. */
        public Person getCoach() {
            return coach;
        }

        public List<Person> getPlayers() {
            return players;
        }
    }
}
