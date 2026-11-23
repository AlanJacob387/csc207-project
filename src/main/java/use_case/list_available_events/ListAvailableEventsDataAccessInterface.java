package use_case.list_available_events;
import java.util.List;
import entity.*;
public interface ListAvailableEventsDataAccessInterface {
 User get(String username);
 List<Event> getAllEvents();
 Team getTeam(String teamId);
}
