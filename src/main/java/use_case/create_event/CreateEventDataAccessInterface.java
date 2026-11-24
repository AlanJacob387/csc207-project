package use_case.create_event;

import entity.Event;

/**
 * Data access interface required by the Create Event use case.
 * Implemented in the data_access layer (DB API, in-memory, etc.).
 */
public interface CreateEventDataAccessInterface {

    /**
     * Saves a new event. If the write fails, nothing is left behind.
     *
     * @param event the event to save
     * @throws java.io.UncheckedIOException if the event could not be written
     */
    void saveEvent(Event event);

    /**
     * Says whether a team exists.
     *
     * @param teamId the team id
     * @return true if there is a team with this id
     */
    boolean teamExists(String teamId);

    /**
     * Looks up who coaches a team.
     *
     * @param teamId the team id
     * @return the coach's username, or null if the team does not exist
     */
    String getCoachUsername(String teamId);
}
