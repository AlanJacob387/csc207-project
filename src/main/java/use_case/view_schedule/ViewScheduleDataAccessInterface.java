package use_case.view_schedule;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import entity.Event;

/**
 * Data access interface required by the View Schedule use case.
 * Implemented in the data_access layer (local JSON file, test fakes).
 *
 * Read-only on purpose: viewing the schedule never changes stored data.
 */
public interface ViewScheduleDataAccessInterface {

    /**
     * Finds the teams a user belongs to, as coach or as player.
     *
     * @param username the user to look up
     * @return the ids of those teams, empty if there are none
     */
    List<String> getTeamIdsForMember(String username);

    /**
     * Finds the events of the given teams that fall on or between two dates.
     *
     * @param teamIds the teams to include
     * @param from    first day, inclusive
     * @param to      last day, inclusive
     * @return the matching events, in no particular order
     */
    List<Event> getEventsForTeams(Collection<String> teamIds, LocalDate from, LocalDate to);
}
