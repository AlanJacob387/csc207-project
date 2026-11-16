package interface_adapter.view_roster;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import use_case.view_roster.ViewRosterOutputData.Person;
import use_case.view_roster.ViewRosterOutputData.TeamRoster;

/** Holds the teams and selectable accounts shown on the Roster page, or why they could not load. */
public class ViewRosterViewModel {
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private List<TeamRoster> teams = List.of();
    private List<Person> coaches = List.of();
    private List<Person> players = List.of();
    private String errorMessage = "";

    public List<TeamRoster> getTeams() {
        return teams;
    }

    public List<Person> getCoaches() {
        return coaches;
    }

    public List<Person> getPlayers() {
        return players;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /** Shows freshly loaded teams and accounts. */
    public void showRoster(List<TeamRoster> newTeams, List<Person> newCoaches, List<Person> newPlayers) {
        teams = newTeams;
        coaches = newCoaches;
        players = newPlayers;
        errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /** Clears everything and shows why it could not be loaded. */
    public void showError(String message) {
        teams = List.of();
        coaches = List.of();
        players = List.of();
        errorMessage = message;
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
