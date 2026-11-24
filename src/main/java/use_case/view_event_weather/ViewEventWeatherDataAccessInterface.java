package use_case.view_event_weather;

import java.util.List;
import entity.Event;
import entity.Team;
import entity.User;

/** Reads the event and current membership used to authorize its forecast. */
public interface ViewEventWeatherDataAccessInterface {
    User get(String username);
    Event getEvent(String eventId);
    Team getTeam(String teamId);
    List<String> getLinkedPlayerUsernames(String parentUsername);
}
