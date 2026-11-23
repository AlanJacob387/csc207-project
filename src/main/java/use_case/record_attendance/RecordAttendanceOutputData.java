package use_case.record_attendance;
/** Count and event identity for a saved roster. */
public final class RecordAttendanceOutputData {
    private final String eventId;
    private final int savedCount;
    public RecordAttendanceOutputData(String eventId, int savedCount) {
        this.eventId = eventId;
        this.savedCount = savedCount;
    }
    public String getEventId() { return eventId; }
    public int getSavedCount() { return savedCount; }
}
