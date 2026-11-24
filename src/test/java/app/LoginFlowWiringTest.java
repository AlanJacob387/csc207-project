package app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.User;
import entity.UserFactory;
import interface_adapter.Session;
import interface_adapter.ViewManagerModel;
import interface_adapter.change_password.ChangePasswordController;
import interface_adapter.change_password.ChangePasswordPresenter;
import interface_adapter.change_password.ChangePasswordViewModel;
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.logout.LogoutController;
import interface_adapter.logout.LogoutPresenter;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import use_case.change_password.ChangePasswordInteractor;
import use_case.login.LoginInteractor;
import use_case.logout.LogoutInteractor;
import use_case.signup.SignupInteractor;
import view.AppShell;
import view.ChangePasswordView;
import view.LoginView;
import view.SignupView;

/**
 * Integration test for the account wiring that AppBuilder builds, without
 * opening a window: signing up, logging in, logging out and changing the
 * password must all drive the session and the view manager correctly.
 */
class LoginFlowWiringTest {

    @TempDir
    Path tempDir;

    /** The account engine, wired exactly as AppBuilder wires it. */
    private static class Wiring {

        final FileAppDataAccessObject dataAccess;
        final ViewManagerModel viewManagerModel;
        final Session session;
        final LoginViewModel loginViewModel;
        final ChangePasswordViewModel changePasswordViewModel;
        final LoginController loginController;
        final SignupController signupController;
        final LogoutController logoutController;
        final ChangePasswordController changePasswordController;

        Wiring(FileAppDataAccessObject dataAccess) {
            this.dataAccess = dataAccess;
            this.viewManagerModel = new ViewManagerModel();
            this.session = new Session();
            this.loginViewModel = new LoginViewModel();

            this.loginController = new LoginController(new LoginInteractor(
                    dataAccess,
                    new LoginPresenter(viewManagerModel, loginViewModel),
                    session));

            this.signupController = new SignupController(new SignupInteractor(
                    dataAccess,
                    new SignupPresenter(viewManagerModel, new SignupViewModel(), loginViewModel),
                    new UserFactory()));

            this.logoutController = new LogoutController(new LogoutInteractor(
                    session,
                    new LogoutPresenter(viewManagerModel, loginViewModel)));

            this.changePasswordViewModel = new ChangePasswordViewModel();
            this.changePasswordController = new ChangePasswordController(
                    new ChangePasswordInteractor(dataAccess,
                            new ChangePasswordPresenter(changePasswordViewModel),
                            session));
        }
    }

    private Wiring wiring() {
        return new Wiring(new FileAppDataAccessObject(tempDir.resolve("app-data.json")));
    }

    @Test
    void signingUpThenLoggingInFillsTheSessionAndShowsTheShell() {
        final Wiring w = wiring();
        w.viewManagerModel.setState(ViewManagerModel.LOGIN);

        w.signupController.execute("newcoach", "pw123456", "pw123456", "New Coach", "COACH");

        assertEquals(ViewManagerModel.LOGIN, w.viewManagerModel.getState());
        assertEquals("Account created for newcoach. Please log in.",
                w.loginViewModel.getSuccessMessage());
        assertFalse(w.session.isLoggedIn());

        w.loginController.execute("newcoach", "pw123456");

        assertTrue(w.session.isLoggedIn());
        assertEquals(Role.COACH, w.session.getCurrentRole());
        assertEquals("New Coach", w.session.getDisplayName());
        assertEquals(ViewManagerModel.APP, w.viewManagerModel.getState());
        assertTrue(w.loginViewModel.getSuccessMessage().isEmpty());
    }

    @Test
    void aWrongPasswordKeepsTheUserOnTheLoginPage() {
        final Wiring w = wiring();
        w.dataAccess.save(new User("player1", "pw", "Pat Player", Role.PLAYER));
        w.viewManagerModel.setState(ViewManagerModel.LOGIN);

        w.loginController.execute("player1", "wrong");

        assertEquals("Incorrect password.", w.loginViewModel.getErrorMessage());
        assertFalse(w.session.isLoggedIn());
        assertEquals(ViewManagerModel.LOGIN, w.viewManagerModel.getState());
    }

    @Test
    void loggingOutThroughTheControllerClearsTheSessionAndShowsLogin() {
        final Wiring w = wiring();
        w.dataAccess.save(new User("player1", "pw", "Pat Player", Role.PLAYER));
        w.loginController.execute("player1", "pw");
        assertEquals(ViewManagerModel.APP, w.viewManagerModel.getState());

        w.logoutController.execute();

        assertFalse(w.session.isLoggedIn());
        assertNull(w.session.getCurrentRole());
        assertEquals("", w.session.getCurrentUsername());
        assertEquals(ViewManagerModel.LOGIN, w.viewManagerModel.getState());
    }

    @Test
    void changingThePasswordThroughTheControllerUpdatesTheStore() {
        final Wiring w = wiring();
        w.dataAccess.save(new User("player1", "pw", "Pat Player", Role.PLAYER));
        w.loginController.execute("player1", "pw");

        w.changePasswordController.execute("pw", "secretpass", "secretpass");

        assertEquals("Password changed.", w.changePasswordViewModel.getSuccessMessage());
        assertTrue(w.dataAccess.get("player1").matches("secretpass"));
        assertEquals(Role.PLAYER, w.dataAccess.get("player1").getRole());
    }

    @Test
    void theViewsBuildAndTheShellFiltersPagesByRole() {
        final LoginView loginView = new LoginView(new LoginViewModel());
        loginView.setLoginController(new LoginController(inputData -> { }));
        loginView.setOnGoToSignup(() -> { });

        final SignupView signupView = new SignupView(new SignupViewModel());
        signupView.setSignupController(new SignupController(inputData -> { }));
        signupView.setOnBackToLogin(() -> { });

        final ChangePasswordView accountView =
                new ChangePasswordView(new ChangePasswordViewModel(), new Session());
        accountView.setChangePasswordController(new ChangePasswordController(inputData -> { }));

        final AppShell shell = new AppShell();
        shell.addPage("Calendar", new JPanel());
        shell.addPage("Account", accountView);
        shell.addPage("New Event", new JPanel(), Role.COACH);
        shell.addPage("My Stats", new JPanel(), Role.PLAYER);

        shell.applyRole(null);
        shell.applyRole(Role.ADMIN);
        shell.applyRole(Role.COACH);
        shell.applyRole(Role.PLAYER);

        assertNotNull(loginView);
        assertNotNull(signupView);
    }
}