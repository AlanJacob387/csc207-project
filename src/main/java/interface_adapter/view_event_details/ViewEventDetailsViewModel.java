package interface_adapter.view_event_details;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.time.LocalDateTime;

/**
 * View model for the event details dialog.
 * Every field is already formatted for display, so the view only copies text
 * into labels. Fires "state" when the details or the error change.
 */
public class ViewEventDetailsViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private String eventId = "";
    private String title = "";
    private String teamName = "";
    private String date = "";
    private String time = "";
    // Unformatted, so an edit form can prefill it in its own input format.
    private LocalDateTime dateTime;
    private String location = "";
    private String opponent = "";
    private boolean game;
    private boolean cancelled;
    private boolean canManage;
    private String errorMessage = "";

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
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

    public boolean isGame() {
        return game;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isCanManage() {
        return canManage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Replaces the details shown and clears any earlier error.
     *
     * @param newEventId   the event id
     * @param newTitle     heading, e.g. "Game vs Rivals"
     * @param newTeamName  team name
     * @param newDate      formatted date
     * @param newTime      formatted start time
     * @param newDateTime  start date and time, unformatted
     * @param newLocation  location
     * @param newOpponent  opponent, or an empty string
     * @param isGame       true for games
     * @param isCancelled  true once the event is cancelled
     * @param mayManage    true when the user may edit or cancel it
     */
    public void setDetails(String newEventId, String newTitle, String newTeamName,
                           String newDate, String newTime, LocalDateTime newDateTime,
                           String newLocation, String newOpponent, boolean isGame,
                           boolean isCancelled, boolean mayManage) {
        this.eventId = newEventId;
        this.title = newTitle;
        this.teamName = newTeamName;
        this.date = newDate;
        this.time = newTime;
        this.dateTime = newDateTime;
        this.location = newLocation;
        this.opponent = newOpponent;
        this.game = isGame;
        this.cancelled = isCancelled;
        this.canManage = mayManage;
        this.errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /**
     * Shows an error. The details are cleared so nothing stale is shown.
     *
     * @param message what went wrong
     */
    public void setErrorMessage(String message) {
        this.eventId = "";
        this.title = "";
        this.teamName = "";
        this.date = "";
        this.time = "";
        this.dateTime = null;
        this.location = "";
        this.opponent = "";
        this.game = false;
        this.cancelled = false;
        this.canManage = false;
        this.errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
