package use_case.export_schedule;

import java.util.Collection;
import java.util.List;

import entity.Event;

/**
 * Data access interface required by the Export Schedule use case, for reading
 * the events to export. Writing the file goes through {@link ScheduleFileWriter}.
 *
 * Implementations report a store they cannot read with IllegalStateException.
 */
public interface ExportScheduleDataAccessInterface {

    /**
     * Finds the teams a user belongs to, as coach or as player.
     *
     * @param username the user to look up
     * @return the ids of those teams, empty if there are none
     */
    List<String> getTeamIdsForMember(String username);

    /**
     * Looks up a team's display name, used to name the exported calendar.
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
