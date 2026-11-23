package use_case.view_event_details;

import java.time.LocalDateTime;

/**
 * Output data for the View Event Details use case.
 */
public class ViewEventDetailsOutputData {
    private final String eventId;
    private final String teamName;
    private final String type;           // "PRACTICE" or "GAME"
    private final LocalDateTime dateTime;
    private final String location;
    private final String opponent;       // null for practices
    private final boolean cancelled;
    private final boolean canManage;

    /**
     * Creates the output data.
     *
     * @param eventId   the event id
     * @param teamName  name of the team the event belongs to
     * @param type      "PRACTICE" or "GAME"
     * @param dateTime  when the event starts
     * @param location  where it happens
     * @param opponent  the opponent, or null
     * @param cancelled whether the event has been cancelled
     * @param canManage true when the signed-in user coaches this team. Only a
     *                  hint for the view; edit and cancel check it again.
     */
    public ViewEventDetailsOutputData(String eventId, String teamName, String type,
                                      LocalDateTime dateTime, String location,
                                      String opponent, boolean cancelled, boolean canManage) {
        this.eventId = eventId;
        this.teamName = teamName;
        this.type = type;
        this.dateTime = dateTime;
        this.location = location;
        this.opponent = opponent;
        this.cancelled = cancelled;
        this.canManage = canManage;
    }

    public String getEventId() {
        return eventId;
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

    public boolean isCanManage() {
        return canManage;
    }
}
