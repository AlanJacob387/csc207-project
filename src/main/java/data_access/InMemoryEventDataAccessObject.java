package data_access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.Event;
import use_case.create_event.CreateEventDataAccessInterface;

/**
 * In-memory implementation of event storage.
 * Useful for tests and for development before the DB API gateway is ready.
 */
public class InMemoryEventDataAccessObject implements CreateEventDataAccessInterface {
    private final Map<String, Event> events = new HashMap<>();
    private final List<String> knownTeamIds = new ArrayList<>();

    public void addTeamId(String teamId) {
        knownTeamIds.add(teamId);
    }

    @Override
    public void saveEvent(Event event) {
        events.put(event.getEventId(), event);
    }

    @Override
    public boolean teamExists(String teamId) {
        return knownTeamIds.contains(teamId);
    }

    public Event getEvent(String eventId) {
        return events.get(eventId);
    }

    public List<Event> getAllEvents() {
        return new ArrayList<>(events.values());
    }
}