package use_case.edit_event;

import entity.Event;

/**
 * Data access interface required by the Edit Event use case.
 * Implemented in the data_access layer (local JSON file, test fakes).
 */
public interface EditEventDataAccessInterface {

    /**
     * Looks up an event. Callers must not modify the returned object; build a
     * changed copy and pass it to {@link #updateEvent(Event)} instead.
     *
     * @param eventId the event id
     * @return the event, or null if there is none
     */
    Event getEvent(String eventId);

    /**
     * Looks up who coaches a team.
     *
     * @param teamId the team id
     * @return the coach's username, or null if the team does not exist
     */
    String getCoachUsername(String teamId);

    /**
     * Replaces the stored event that has the same id. If the write fails, the
     * stored event is left as it was.
     *
     * @param updated the changed event
     * @throws java.io.UncheckedIOException if the change could not be saved
     */
    void updateEvent(Event updated);
}
