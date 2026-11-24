package use_case.rsvp;
import java.util.List;
import entity.*;
public interface RsvpDataAccessInterface {
 User get(String username);
 java.util.List<Event> getAllEvents();
 Event getEvent(String eventId);
 Team getTeam(String teamId);
 void saveRsvp(RsvpRecord record);
 List<RsvpRecord> getRsvpsForEvent(String eventId);
}
