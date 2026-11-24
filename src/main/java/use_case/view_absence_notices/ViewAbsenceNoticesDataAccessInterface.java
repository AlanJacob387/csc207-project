package use_case.view_absence_notices;

import java.util.List;
import entity.AbsenceNotice;
import entity.Event;
import entity.Team;
import entity.User;

/** Reads current links, teams, events, and notices for an authorized snapshot. */
public interface ViewAbsenceNoticesDataAccessInterface {
    User get(String username);
    List<Team> getAllTeams();
    List<Event> getAllEvents();
    List<String> getLinkedPlayerUsernames(String parentUsername);
    List<AbsenceNotice> getAbsenceNotices();
}
