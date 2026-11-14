package use_case.view_event_details;

import entity.Event;

/**
 * Data access interface required by the View Event Details use case.
 * Implemented in the data_access layer (local JSON file, test fakes).
 *
 * Read-only: showing an event never changes it.
 */
public interface ViewEventDetailsDataAccessInterface {

    /**
     * Looks up an event.
     *
     * @param eventId the event id
     * @return the event, or null if there is none
     */
    Event getEvent(String eventId);

    /**
     * Looks up a team's display name.
     *
     * @param teamId the team id
     * @return the name, or null if the team does not exist
     */
    String getTeamName(String teamId);

    /**
     * Looks up who coaches a team.
     *
     * @param teamId the team id
     * @return the coach's username, or null if the team does not exist
     */
    String getCoachUsername(String teamId);
}
