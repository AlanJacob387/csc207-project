package use_case.link_parent_to_player;

import java.util.List;
import entity.ParentPlayerLink;
import entity.User;

/** Lists eligible accounts and persists administrator-approved parent links. */
public interface LinkParentToPlayerDataAccessInterface {
    User get(String username);
    List<User> getAllUsers();
    List<ParentPlayerLink> getParentPlayerLinks();
    void saveParentPlayerLink(ParentPlayerLink link);
    void deleteParentPlayerLink(String parentUsername, String playerUsername);
}
