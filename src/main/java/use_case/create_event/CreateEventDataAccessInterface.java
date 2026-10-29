package use_case.create_event;

import entity.Event;

/**
 * Data access interface required by the Create Event use case.
 * Implemented in the data_access layer (DB API, in-memory, etc.).
 */
public interface CreateEventDataAccessInterface {
    void saveEvent(Event event);

    boolean teamExists(String teamId);
}