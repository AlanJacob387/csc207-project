package use_case.link_parent_to_player;

import java.util.List;

/** Account choices and the current approved relationships, without credentials. */
public record LinkParentToPlayerOutputData(List<AccountRow> parents, List<AccountRow> players,
                                          List<LinkRow> links, String message) {
    public LinkParentToPlayerOutputData {
        parents = List.copyOf(parents);
        players = List.copyOf(players);
        links = List.copyOf(links);
    }

    public record AccountRow(String username, String displayName) { }
    public record LinkRow(String parentUsername, String parentName, String playerUsername, String playerName) { }
}
