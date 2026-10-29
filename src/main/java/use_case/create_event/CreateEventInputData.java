package use_case.create_event;

import java.time.LocalDateTime;

/**
 * Input data for the Create Event use case.
 */
public class CreateEventInputData {
    private final String teamId;
    private final String eventType;      // "PRACTICE" or "GAME"
    private final LocalDateTime dateTime;
    private final String location;
    private final String opponent;       // may be null

    public CreateEventInputData(String teamId, String eventType,
                                LocalDateTime dateTime, String location, String opponent) {
        this.teamId = teamId;
        this.eventType = eventType;
        this.dateTime = dateTime;
        this.location = location;
        this.opponent = opponent;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getEventType() {
        return eventType;
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
}