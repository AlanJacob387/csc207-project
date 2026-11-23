package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.User;
import entity.UserFactory;
import interface_adapter.Session;
import interface_adapter.ViewManagerModel;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.manage_users.ManageUsersController;
import interface_adapter.manage_users.ManageUsersPresenter;
import interface_adapter.manage_users.ManageUsersViewModel;
import use_case.login.LoginInteractor;
import use_case.manage_users.ChangeUserRoleInteractor;
import use_case.manage_users.CreateUserInteractor;
import use_case.manage_users.DeactivateUserInteractor;

/**
 * Integration test for the administrator page wired the way AppBuilder wires it,
 * without opening a window: an account created there can sign in, a role change is
 * visible at the next sign-in, and a deactivated account is refused.
 */
class ManageUsersFlowTest {

    @TempDir
    Path tempDir;

    /** The account engine, wired exactly as AppBuilder wires it. */
    private static class Wiring {

        final FileAppDataAccessObject dataAccess;
        final Session session = new Session();
        final LoginViewModel loginViewModel = new LoginViewModel();
        final ManageUsersViewModel manageUsersViewModel = new ManageUsersViewModel();
        final LoginController loginController;
        final ManageUsersController manageUsersController;

        Wiring(FileAppDataAccessObject dataAccess) {
            this.dataAccess = dataAccess;
            final ViewManagerModel viewManagerModel = new ViewManagerModel();
            this.loginController = new LoginController(new LoginInteractor(dataAccess,
                    new LoginPresenter(viewManagerModel, loginViewModel), session));

            final ManageUsersPresenter presenter = new ManageUsersPresenter(manageUsersViewModel);
            this.manageUsersController = new ManageUsersController(
                    new CreateUserInteractor(dataAccess, presenter, session, new UserFactory()),
                    new ChangeUserRoleInteractor(dataAccess, presenter, session),
                    new DeactivateUserInteractor(dataAccess, presenter, session));
        }

        void signInAs(String username, String password) {
            loginController.execute(username, password);
        }
    }

    private Wiring wiring() {
        final Wiring wiring = new Wiring(new FileAppDataAccessObject(tempDir.resolve("app-data.json")));
        wiring.dataAccess.save(new User("admin1", "pw", "Ada Admin", Role.ADMIN));
        return wiring;
    }

    @Test
    void anAccountCreatedOnThePageCanSignIn() {
        final Wiring w = wiring();
        w.signInAs("admin1", "pw");

        w.manageUsersController.createUser("player9", "Pat Nine", "secret", "secret", "PLAYER");

        assertEquals("Account created for player9.", w.manageUsersViewModel.getSuccessMessage());
        w.session.clearCurrentUser();
        w.signInAs("player9", "secret");
        assertEquals(Role.PLAYER, w.session.getCurrentRole());
        assertEquals("Pat Nine", w.session.getDisplayName());
    }

    @Test
    void aRoleChangeIsVisibleAtTheNextSignIn() {
        final Wiring w = wiring();
        w.dataAccess.save(new User("player9", "secret", "Pat Nine", Role.PLAYER));
        w.signInAs("admin1", "pw");

        w.manageUsersController.changeUserRole("player9", "COACH");

        assertEquals("Role changed: player9 is now COACH.",
                w.manageUsersViewModel.getSuccessMessage());
        w.session.clearCurrentUser();
        w.signInAs("player9", "secret");
        assertEquals(Role.COACH, w.session.getCurrentRole());
    }

    @Test
    void aNonAdministratorCannotReachTheActions() {
        final Wiring w = wiring();
        w.dataAccess.save(new User("player1", "pw", "Pat Player", Role.PLAYER));
        w.signInAs("player1", "pw");

        w.manageUsersController.createUser("sneaky", "", "secret", "secret", "ADMIN");

        assertEquals("Only an administrator can manage users.",
                w.manageUsersViewModel.getErrorMessage());
        assertNull(w.dataAccess.get("sneaky"));
    }

    @Test
    void aDeactivatedAccountIsRefusedAtTheNextSignIn() {
        final Wiring w = wiring();
        w.dataAccess.save(new User("player1", "pw", "Pat Player", Role.PLAYER));
        w.signInAs("admin1", "pw");

        w.manageUsersController.deactivateUser("player1");

        assertFalse(w.dataAccess.get("player1").isActive());
        w.session.clearCurrentUser();
        w.signInAs("player1", "pw");
        assertEquals("This account has been deactivated.", w.loginViewModel.getErrorMessage());
        assertFalse(w.session.isLoggedIn());
    }

    @Test
    void theAdministratorCannotDeactivateThemselves() {
        final Wiring w = wiring();
        w.signInAs("admin1", "pw");

        w.manageUsersController.deactivateUser("admin1");

        assertEquals("You cannot deactivate your own account.",
                w.manageUsersViewModel.getErrorMessage());
        assertTrue(w.dataAccess.get("admin1").isActive());
    }
}
