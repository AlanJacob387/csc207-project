package interface_adapter.view_schedule;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * View model for the calendar.
 * Holds the entries for the range on screen plus a message for the user, and
 * fires "state" whenever either changes.
 */
public class ViewScheduleViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private List<Entry> entries = new ArrayList<>();
    private String message = "";
    private boolean error;

    public List<Entry> getEntries() {
        return new ArrayList<>(entries);
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return error;
    }

    /**
     * Replaces the entries shown and the informational message.
     *
     * @param newEntries entries for the range on screen
     * @param newMessage message to show, or an empty string for none
     */
    public void setSchedule(List<Entry> newEntries, String newMessage) {
        this.entries = new ArrayList<>(newEntries);
        this.message = newMessage;
        this.error = false;
        support.firePropertyChange("state", null, this);
    }

    /**
     * Clears the entries and shows an error instead.
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
     * One event, already formatted for display.
     */
    public static class Entry {
        private final String eventId;
        private final String title;
        private final boolean game;
        private final LocalDate date;
        private final LocalTime startTime;

        /**
         * Creates an entry.
         *
         * @param eventId   the event id, kept so the details page can find it later
         * @param title     text shown on the calendar
         * @param game      true for games, false for practices
         * @param date      day the event is on
         * @param startTime when it starts
         */
        public Entry(String eventId, String title, boolean game,
                     LocalDate date, LocalTime startTime) {
            this.eventId = eventId;
            this.title = title;
            this.game = game;
            this.date = date;
            this.startTime = startTime;
        }

        public String getEventId() {
            return eventId;
        }

        public String getTitle() {
            return title;
        }

        public boolean isGame() {
            return game;
        }

        public LocalDate getDate() {
            return date;
        }

        public LocalTime getStartTime() {
            return startTime;
        }
    }
}
