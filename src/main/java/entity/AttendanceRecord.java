package entity;

/**
 * One player's attendance (and optional performance stats) for one event.
 */
public class AttendanceRecord {
    public enum Status { PRESENT, ABSENT, EXCUSED }

    private final String eventId;
    private final String playerUsername;
    private Status status;
    private String statsNotes; // e.g. "2 goals, 1 assist" — refine into structured stats later

    public AttendanceRecord(String eventId, String playerUsername, Status status) {
        this.eventId = eventId;
        this.playerUsername = playerUsername;
        this.status = status;
    }

    public String getEventId() {
        return eventId;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getStatsNotes() {
        return statsNotes;
    }

    public void setStatsNotes(String statsNotes) {
        this.statsNotes = statsNotes;
    }
}