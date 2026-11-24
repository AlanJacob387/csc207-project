package use_case.view_notifications;

import java.util.List;

import entity.EventNotification;
import entity.Team;
import entity.User;

/** Reads current membership and the durable event change log. */
public interface ViewNotificationsDataAccessInterface {
    User get(String username);

    List<Team> getAllTeams();

    List<String> getLinkedPlayerUsernames(String parentUsername);

    List<EventNotification> getEventNotifications();
}
