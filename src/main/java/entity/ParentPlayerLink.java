package entity;

import java.util.Objects;

/** An administrator-approved relationship between a parent and a player. */
public final class ParentPlayerLink {
    private final String parentUsername;
    private final String playerUsername;

    public ParentPlayerLink(String parentUsername, String playerUsername) {
        this.parentUsername = Objects.requireNonNull(parentUsername);
        this.playerUsername = Objects.requireNonNull(playerUsername);
    }

    public String getParentUsername() {
        return parentUsername;
    }

    public String getPlayerUsername() {
        return playerUsername;
    }
}
