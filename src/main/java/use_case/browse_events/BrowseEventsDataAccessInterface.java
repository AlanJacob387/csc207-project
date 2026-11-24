package use_case.browse_events;

import java.util.Collection;
import java.util.List;

import entity.Event;

/**
 * Data access interface required by the Browse Events use case.
 * Implemented in the data_access layer (local JSON file, test fakes).
 *
 * Read-only: browsing never changes stored data.
 */
public interface BrowseEventsDataAccessInterface {

    /**
     * Finds the teams a user belongs to, as coach or as player.
     *
     * @param username the user to look up
     * @return the ids of those teams, empty if there are none
     */
    List<String> getTeamIdsForMember(String username);

    /**
     * Looks up a team's display name.
     *
     * @param teamId the team id
     * @return the name, or null if the team does not exist
     */
    String getTeamName(String teamId);

    /**
     * Finds every event of the given teams, past and future.
     *
     * @param teamIds the teams to include
     * @return the matching events, in no particular order
     */
    List<Event> getEventsForTeams(Collection<String> teamIds);
}
