package interface_adapter.browse_events;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

/**
 * View model for the Events list page.
 * Holds the rows to show, the teams offered as filters, and a message for when
 * the list is empty or something went wrong. Fires "state" on every update.
 */
public class BrowseEventsViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private List<Entry> entries = new ArrayList<>();
    private List<TeamChoice> teams = new ArrayList<>();
    private String message = "";
    private boolean error;

    public List<Entry> getEntries() {
        return new ArrayList<>(entries);
    }

    public List<TeamChoice> getTeams() {
        return new ArrayList<>(teams);
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return error;
    }

    /**
     * Replaces the list.
     *
     * @param newEntries rows to show
     * @param newTeams   teams to offer in the team filter
     * @param newMessage message for an empty list, or an empty string
     */
    public void setEvents(List<Entry> newEntries, List<TeamChoice> newTeams, String newMessage) {
        this.entries = new ArrayList<>(newEntries);
        this.teams = new ArrayList<>(newTeams);
        this.message = newMessage;
        this.error = false;
        support.firePropertyChange("state", null, this);
    }

    /**
     * Clears the rows and shows an error. The team choices are kept so the
     * filters stay usable.
     *
     * @param errorMessage what went wrong
     */
    public void setError(String errorMessage) {
        this.entries = new ArrayList<>();
        this.message = errorMessage;
        this.error = true;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    /**
     * A team in the team filter.
     */
    public static class TeamChoice {
        private final String teamId;
        private final String name;

        /**
         * Creates a choice.
         *
         * @param teamId the team id sent back when it is picked
         * @param name   the label shown
         */
        public TeamChoice(String teamId, String name) {
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
     * One row of the list, already formatted for display.
     */
    public static class Entry {
        private final String eventId;
        private final String date;
        private final String time;
        private final String title;
        private final String teamName;
        private final String location;
        private final boolean cancelled;

        /**
         * Creates a row.
         *
         * @param eventId   the event id, used to open its details
         * @param date      formatted date
         * @param time      formatted start time
         * @param title     e.g. "Game vs Rivals" or "Cancelled: Practice"
         * @param teamName  team name
         * @param location  location
         * @param cancelled true if the event has been cancelled
         */
        public Entry(String eventId, String date, String time, String title,
                     String teamName, String location, boolean cancelled) {
            this.eventId = eventId;
            this.date = date;
            this.time = time;
            this.title = title;
            this.teamName = teamName;
            this.location = location;
            this.cancelled = cancelled;
        }

        public String getEventId() {
            return eventId;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }

        public String getTitle() {
            return title;
        }

        public String getTeamName() {
            return teamName;
        }

        public String getLocation() {
            return location;
        }

        public boolean isCancelled() {
            return cancelled;
        }
    }
}
