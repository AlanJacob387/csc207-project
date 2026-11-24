package use_case.flag_rain_risk;

import java.util.List;
import entity.Event;
import entity.Team;
import entity.User;

/** Narrow independent reads for the upcoming-event rain overview. */
public interface FlagRainRiskDataAccessInterface {
    User get(String username);
    Event getEvent(String eventId);
    Team getTeam(String teamId);
    List<Event> getAllEvents();
    List<String> getLinkedPlayerUsernames(String parentUsername);
}
