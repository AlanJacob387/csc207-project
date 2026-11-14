package use_case.browse_events;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Output data for the Browse Events use case.
 *
 * Besides the matching events it lists the user's teams, so the view can offer
 * them as filter choices without asking the store itself.
 */
public class BrowseEventsOutputData {
    private final List<EventRow> events;
    private final List<TeamOption> teams;
    private final boolean onAnyTeam;
    private final boolean filtered;

    /**
     * Creates the output data.
     *
     * @param events    matching events, earliest first
     * @param teams     every team the user belongs to
     * @param onAnyTeam false when the user belongs to no team
     * @param filtered  true when a type or team filter was applied
     */
    public BrowseEventsOutputData(List<EventRow> events, List<TeamOption> teams,
                                  boolean onAnyTeam, boolean filtered) {
        this.events = new ArrayList<>(events);
        this.teams = new ArrayList<>(teams);
        this.onAnyTeam = onAnyTeam;
        this.filtered = filtered;
    }

    public List<EventRow> getEvents() {
        return new ArrayList<>(events);
    }

    public List<TeamOption> getTeams() {
        return new ArrayList<>(teams);
    }

    public boolean isOnAnyTeam() {
        return onAnyTeam;
    }

    public boolean isFiltered() {
        return filtered;
    }

    /**
     * A team the user can filter by.
     */
    public static class TeamOption {
        private final String teamId;
        private final String name;

        /**
         * Creates a team option.
         *
         * @param teamId the team id
         * @param name   the team's display name
         */
        public TeamOption(String teamId, String name) {
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

    /**
     * One event in the list.
     */
    public static class EventRow {
        private final String eventId;
        private final String teamId;
        private final String teamName;
        private final String type;           // "PRACTICE" or "GAME"
        private final LocalDateTime dateTime;
        private final String location;
        private final String opponent;       // null for practices
        private final boolean cancelled;

        /**
         * Creates a row.
         *
         * @param eventId   the event id
         * @param teamId    the team the event belongs to
         * @param teamName  that team's display name
         * @param type      "PRACTICE" or "GAME"
         * @param dateTime  when the event starts
         * @param location  where it happens
         * @param opponent  the opponent, or null
         * @param cancelled whether the event has been cancelled
         */
        public EventRow(String eventId, String teamId, String teamName, String type,
                        LocalDateTime dateTime, String location, String opponent,
                        boolean cancelled) {
            this.eventId = eventId;
            this.teamId = teamId;
            this.teamName = teamName;
            this.type = type;
            this.dateTime = dateTime;
            this.location = location;
            this.opponent = opponent;
            this.cancelled = cancelled;
        }

        public String getEventId() {
            return eventId;
        }

        public String getTeamId() {
            return teamId;
        }

        public String getTeamName() {
            return teamName;
        }

        public String getType() {
            return type;
        }

        public LocalDateTime getDateTime() {
            return dateTime;
        }

        public String getLocation() {
            return location;
        }

        public String getOpponent() {
            return opponent;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
