package entity;

import java.time.LocalDateTime;

/**
 * A scheduled team event: a practice or a game.
 */
public class Event {
    public enum EventType { PRACTICE, GAME }

    private final String eventId;
    private final String teamId;
    private EventType type;
    private LocalDateTime dateTime;
    private String location;
    private String opponent;     // null for practices
    private boolean cancelled;

    public Event(String eventId, String teamId, EventType type,
                 LocalDateTime dateTime, String location, String opponent) {
        this.eventId = eventId;
        this.teamId = teamId;
        this.type = type;
        this.dateTime = dateTime;
        this.location = location;
        this.opponent = opponent;
        this.cancelled = false;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTeamId() {
        return teamId;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOpponent() {
        return opponent;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void cancel() {
        this.cancelled = true;
    }
}