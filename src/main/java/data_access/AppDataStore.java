package data_access;

import java.util.List;

import entity.AttendanceRecord;
import entity.Event;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.change_password.ChangePasswordUserDataAccessInterface;
import use_case.create_event.CreateEventDataAccessInterface;
import use_case.login.LoginUserDataAccessInterface;
import use_case.post_announcement.PostAnnouncementDataAccessInterface;
import use_case.signup.SignupUserDataAccessInterface;
import use_case.view_announcements.ViewAnnouncementsDataAccessInterface;

/**
 * Everything the application needs from its store.
 *
 * The use cases keep their own narrow interfaces and know nothing about this
 * one. It exists only for the wiring: AppBuilder holds one field of this type,
 * so the local JSON file and the hosted database are interchangeable and no
 * interactor changes when the store does.
 *
 * A class implementing this is expected to keep the same meaning for every
 * method - a save replaces the record with the same key, and saveAnnouncement
 * refuses a duplicate id instead of replacing it.
 */
public interface AppDataStore extends
        LoginUserDataAccessInterface,
        SignupUserDataAccessInterface,
        ChangePasswordUserDataAccessInterface,
        CreateEventDataAccessInterface,
        PostAnnouncementDataAccessInterface,
        ViewAnnouncementsDataAccessInterface {

    /**
     * Returns every stored account.
     *
     * @return the accounts, in a stable order
     */
    List<User> getAllUsers();

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

    /**
     * Describes the store in one line, so the sign-in page can show whether the
     * app is on the shared database or on a local file.
     *
     * @return a short description, such as "Local file: data/app-data.json"
     */
    String describe();
}
