package data_access;

import java.util.HashMap;
import java.util.Map;

import entity.User;
import use_case.login.LoginUserDataAccessInterface;
import use_case.change_password.ChangePasswordUserDataAccessInterface;
import use_case.signup.SignupUserDataAccessInterface;

/**
 * In-memory user storage, used by unit tests.
 *
 * Nothing is written to disk or to the network, so tests can run anywhere and
 * a use case can be programmed before the real persistence is finished.
 */
public class InMemoryUserDataAccessObject
        implements LoginUserDataAccessInterface, SignupUserDataAccessInterface,
                ChangePasswordUserDataAccessInterface {

    private final Map<String, User> users = new HashMap<>();

    @Override
    public User get(String username) {
        return users.get(username);
    }

    @Override
    public boolean existsByName(String username) {
        return users.containsKey(username);
    }

    @Override
    public void save(User user) {
        users.put(user.getUsername(), user);
    }
}
