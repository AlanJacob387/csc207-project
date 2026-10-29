package entity;

import java.util.ArrayList;
import java.util.List;

/**
 * A sports team: one coach, many players.
 */
public class Team {
    private final String teamId;
    private String name;
    private String sport;
    private String coachUsername;
    private final List<String> playerUsernames = new ArrayList<>();

    public Team(String teamId, String name, String sport, String coachUsername) {
        this.teamId = teamId;
        this.name = name;
        this.sport = sport;
        this.coachUsername = coachUsername;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSport() {
        return sport;
    }

    public void setSport(String sport) {
        this.sport = sport;
    }

    public String getCoachUsername() {
        return coachUsername;
    }

    public void setCoachUsername(String coachUsername) {
        this.coachUsername = coachUsername;
    }

    public List<String> getPlayerUsernames() {
        return new ArrayList<>(playerUsernames);
    }

    public void addPlayer(String username) {
        if (!playerUsernames.contains(username)) {
            playerUsernames.add(username);
        }
    }

    public void removePlayer(String username) {
        playerUsernames.remove(username);
    }
}