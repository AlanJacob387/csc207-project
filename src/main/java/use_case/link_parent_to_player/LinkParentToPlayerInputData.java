package use_case.link_parent_to_player;

/** Requests load, link, or unlink using an administrator's current session. */
public record LinkParentToPlayerInputData(String action, String parentUsername, String playerUsername) {
}
