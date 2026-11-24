package use_case.view_schedule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Output data for the View Schedule use case.
 *
 * Events are copied into plain rows so the Event entity never reaches the
 * presenter.
 */
public class ViewScheduleOutputData {
    private final List<EventRow> events;
    private final boolean onAnyTeam;

    /**
     * Creates the output data.
     *
     * @param events    the events in the range, earliest first
     * @param onAnyTeam false when the user belongs to no team, so the view can
     *                  say why the calendar is empty
     */
    public ViewScheduleOutputData(List<EventRow> events, boolean onAnyTeam) {
        this.events = new ArrayList<>(events);
        this.onAnyTeam = onAnyTeam;
    }

    public List<EventRow> getEvents() {
        return new ArrayList<>(events);
    }

    public boolean isOnAnyTeam() {
        return onAnyTeam;
    }

    /**
     * One scheduled event as the use case reports it.
     */
    public static class EventRow {
        private final String eventId;
        private final String teamId;
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
         * @param type      "PRACTICE" or "GAME"
         * @param dateTime  when the event starts
         * @param location  where it happens
         * @param opponent  the opponent, or null
         * @param cancelled whether the event has been cancelled
         */
        public EventRow(String eventId, String teamId, String type, LocalDateTime dateTime,
                        String location, String opponent, boolean cancelled) {
            this.eventId = eventId;
            this.teamId = teamId;
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
