package data_access;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.Role;
import entity.User;
import use_case.accounts.DuplicateUsernameException;
import use_case.change_password.ChangePasswordUserDataAccessInterface;
import use_case.login.LoginUserDataAccessInterface;
import use_case.manage_users.CreateUserDataAccessInterface;
import use_case.manage_users.ManageUsersDataAccessInterface;
import use_case.signup.SignupUserDataAccessInterface;

/**
 * In-memory user storage, used by unit tests.
 *
 * Nothing is written to disk or to the network, so tests can run anywhere and
 * a use case can be programmed before the real persistence is finished.
 */
public class InMemoryUserDataAccessObject
        implements LoginUserDataAccessInterface, SignupUserDataAccessInterface,
                ChangePasswordUserDataAccessInterface, ManageUsersDataAccessInterface,
                CreateUserDataAccessInterface {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public User get(String username) {
        return users.get(username);
    }

    @Override
    public boolean existsByName(String username) {
        return users.containsKey(username);
    }

    /**
     * Stores a new account, refusing a username that is already taken.
     *
     * @param user the account to create
     * @throws DuplicateUsernameException when that username is already stored
     */
    @Override
    public void createUser(User user) {
        if (users.containsKey(user.getUsername())) {
            throw new DuplicateUsernameException(user.getUsername());
        }
        users.put(user.getUsername(), user);
    }

    @Override
    public void save(User user) {
        users.put(user.getUsername(), user);
    }

    @Override
    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void updateRole(String username, Role role) {
        users.put(username, existing(username).withRole(role));
    }

    @Override
    public void updateActive(String username, boolean active) {
        users.put(username, existing(username).withActive(active));
    }

    private User existing(String username) {
        final User user = users.get(username);
        if (user == null) {
            throw new IllegalStateException("No account found for " + username + ".");
        }
        return user;
    }
}
