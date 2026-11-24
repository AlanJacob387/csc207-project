package data_access;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.browse_events.BrowseEventsDataAccessInterface;
import use_case.cancel_event.CancelEventDataAccessInterface;
import use_case.change_password.ChangePasswordUserDataAccessInterface;
import use_case.create_event.CreateEventDataAccessInterface;
import use_case.edit_event.EditEventDataAccessInterface;
import use_case.export_schedule.ExportScheduleDataAccessInterface;
import use_case.flag_rain_risk.FlagRainRiskDataAccessInterface;
import use_case.link_parent_to_player.LinkParentToPlayerDataAccessInterface;
import use_case.login.LoginUserDataAccessInterface;
import use_case.manage_users.ManageUsersDataAccessInterface;
import use_case.post_announcement.PostAnnouncementDataAccessInterface;
import use_case.signup.SignupUserDataAccessInterface;
import use_case.submit_absence_notice.SubmitAbsenceNoticeDataAccessInterface;
import use_case.view_absence_notices.ViewAbsenceNoticesDataAccessInterface;
import use_case.view_announcements.ViewAnnouncementsDataAccessInterface;
import use_case.view_event_details.ViewEventDetailsDataAccessInterface;
import use_case.view_event_weather.ViewEventWeatherDataAccessInterface;
import use_case.view_notifications.ViewNotificationsDataAccessInterface;
import use_case.view_schedule.ViewScheduleDataAccessInterface;

/**
 * Everything the application needs from its store.
 *
 * The use cases keep their own narrow interfaces and know nothing about this
 * one. It exists only for the wiring: AppBuilder holds one field of this type,
 * so the local JSON file and the hosted database are interchangeable and no
 * interactor changes when the store does.
 *
 * A class implementing this is expected to keep the same meaning for every
 * method - a save replaces the record with the same key, saveAnnouncement
 * refuses a duplicate id instead of replacing it, and saving an event whose
 * schedule changed also records the change for the notifications feed.
 */
public interface AppDataStore extends
        LoginUserDataAccessInterface,
        SignupUserDataAccessInterface,
        ChangePasswordUserDataAccessInterface,
        CreateEventDataAccessInterface,
        PostAnnouncementDataAccessInterface,
        ViewAnnouncementsDataAccessInterface,
        ManageUsersDataAccessInterface,
        ViewScheduleDataAccessInterface,
        ViewEventDetailsDataAccessInterface,
        EditEventDataAccessInterface,
        CancelEventDataAccessInterface,
        BrowseEventsDataAccessInterface,
        ExportScheduleDataAccessInterface,
        LinkParentToPlayerDataAccessInterface,
        SubmitAbsenceNoticeDataAccessInterface,
        ViewAbsenceNoticesDataAccessInterface,
        ViewNotificationsDataAccessInterface,
        ViewEventWeatherDataAccessInterface,
        FlagRainRiskDataAccessInterface {

    /**
     * Updates a user's display name and role, leaving the credential and the
     * active flag as they are.
     *
     * @param username    the user's username
     * @param displayName the name to display
     * @param role        the user's role
     */
    void saveProfile(String username, String displayName, Role role);

    /**
     * Saves a team, replacing the stored one with the same id.
     *
     * @param team the team to save
     */
    void saveTeam(Team team);

    /**
     * Looks up an event.
     *
     * @param eventId the event id
     * @return the event, or null if there is none
     */
    Event getEvent(String eventId);

    /**
     * Returns every stored event.
     *
     * @return the events
     */
    List<Event> getAllEvents();

    /**
     * Saves one player's attendance for one event.
     *
     * @param record the record to save
     */
    void saveAttendance(AttendanceRecord record);

    /**
     * Returns the attendance records stored for an event.
     *
     * @param eventId the event id
     * @return the matching records
     */
    List<AttendanceRecord> getAttendanceFor(String eventId);

    // ------------------------------------------------------------------
    // Questions every store answers the same way. They are built from the
    // stored accounts, teams, events and family links, so a store only has to
    // supply those; the local file overrides them to answer under its lock.
    // ------------------------------------------------------------------

    @Override
    default List<String> getLinkedPlayerUsernames(String parentUsername) {
        return getParentPlayerLinks().stream()
                .filter(link -> link.getParentUsername().equals(parentUsername))
                .map(entity.ParentPlayerLink::getPlayerUsername).toList();
    }

    @Override
    default List<String> getTeamIdsForMember(String username) {
        final List<String> teamIds = new ArrayList<>();
        final User user = get(username);
        if (user == null) {
            return teamIds;
        }
        final List<String> children = user.getRole() == Role.PARENT
                ? getLinkedPlayerUsernames(username) : List.of();
        for (Team team : getAllTeams()) {
            if (user.getRole() == Role.ADMIN
                    || user.getRole() == Role.COACH && username.equals(team.getCoachUsername())
                    || user.getRole() == Role.PLAYER && team.getPlayerUsernames().contains(username)
                    || user.getRole() == Role.PARENT
                        && children.stream().anyMatch(team.getPlayerUsernames()::contains)) {
                teamIds.add(team.getTeamId());
            }
        }
        return teamIds;
    }

    @Override
    default boolean canManageEvent(String username, String teamId) {
        final User user = get(username);
        final Team team = getTeam(teamId);
        return user != null && user.getRole() == Role.COACH && team != null
                && user.getUsername().equals(team.getCoachUsername());
    }

    @Override
    default List<Event> getEventsForTeams(Collection<String> teamIds, LocalDate from, LocalDate to) {
        final List<Event> matches = new ArrayList<>();
        for (Event event : getAllEvents()) {
            if (event.getDateTime() == null || !teamIds.contains(event.getTeamId())) {
                continue;
            }
            final LocalDate date = event.getDateTime().toLocalDate();
            if (!date.isBefore(from) && !date.isAfter(to)) {
                matches.add(event);
            }
        }
        return matches;
    }

    @Override
    default List<Event> getEventsForTeams(Collection<String> teamIds) {
        final List<Event> matches = new ArrayList<>();
        for (Event event : getAllEvents()) {
            if (teamIds.contains(event.getTeamId())) {
                matches.add(event);
            }
        }
        return matches;
    }

    @Override
    default String getTeamName(String teamId) {
        final Team team = getTeam(teamId);
        return team == null ? null : team.getName();
    }

    @Override
    default String getCoachUsername(String teamId) {
        final Team team = getTeam(teamId);
        return team == null ? null : team.getCoachUsername();
    }

    @Override
    default void updateEvent(Event updated) {
        saveEvent(updated);
    }

    /**
     * Describes the store in one line, so the sign-in page can show whether the
     * app is on the shared database or on a local file. A store that had to recover
     * from a damaged file adds a short note about where the old file went.
     *
     * @return a short description, such as "Local file: data/app-data.json"
     */
    String describe();
}
