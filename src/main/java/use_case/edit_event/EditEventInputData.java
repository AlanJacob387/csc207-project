package use_case.edit_event;

import java.time.LocalDateTime;

/**
 * Input data for the Edit Event use case. Every editable field is sent, even
 * the ones that did not change.
 */
public class EditEventInputData {
    private final String eventId;
    private final LocalDateTime dateTime;
    private final String location;
    private final String opponent;       // ignored for practices

    /**
     * Creates the input data.
     *
     * @param eventId  the event being edited
     * @param dateTime new start date and time
     * @param location new location
     * @param opponent new opponent, or blank for none
     */
    public EditEventInputData(String eventId, LocalDateTime dateTime,
                              String location, String opponent) {
        this.eventId = eventId;
        this.dateTime = dateTime;
        this.location = location;
        this.opponent = opponent;
    }

    public String getEventId() {
        return eventId;
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
