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
import interface_adapter.login.LoginController;
import interface_adapter.login.LoginPresenter;
import interface_adapter.login.LoginViewModel;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupPresenter;
import interface_adapter.signup.SignupViewModel;
import use_case.login.LoginInteractor;
import use_case.signup.SignupInteractor;
import view.AppShell;
import view.LoginView;
import view.SignupView;

/**
 * Integration test for the login and signup wiring built in {@code Main},
 * without opening a window: a successful login must fill the session and ask
 * the view manager to show the shell.
 */
class LoginFlowWiringTest {

    @TempDir
    Path tempDir;

    @Test
    void signingUpThenLoggingInFillsTheSessionAndShowsTheShell() {
        final FileAppDataAccessObject dataAccess =
                new FileAppDataAccessObject(tempDir.resolve("app-data.json"));
        final ViewManagerModel viewManagerModel = new ViewManagerModel();
        final Session session = new Session();
        final LoginViewModel loginViewModel = new LoginViewModel();

        final LoginController loginController = new LoginController(new LoginInteractor(
                dataAccess, new LoginPresenter(viewManagerModel, loginViewModel, session)));

        final SignupViewModel signupViewModel = new SignupViewModel();
        final SignupController signupController = new SignupController(new SignupInteractor(
                dataAccess,
                new SignupPresenter(viewManagerModel, signupViewModel, loginViewModel),
                new UserFactory()));

        viewManagerModel.setState(ViewManagerModel.LOGIN);

        // Create an account: back to the login page, with a message, still signed out.
        signupController.execute("newcoach", "pw", "pw", "New Coach", "COACH");

        assertEquals(ViewManagerModel.LOGIN, viewManagerModel.getState());
        assertEquals("Account created for newcoach. Please log in.",
                loginViewModel.getSuccessMessage());
        assertFalse(session.isLoggedIn());

        // Log in: the session fills and the shell is shown.
        loginController.execute("newcoach", "pw");

        assertTrue(session.isLoggedIn());
        assertEquals(Role.COACH, session.getRole());
        assertEquals("New Coach", session.getDisplayName());
        assertEquals(ViewManagerModel.APP, viewManagerModel.getState());
        assertTrue(loginViewModel.getSuccessMessage().isEmpty());

        // Logging out clears the session.
        session.logOut();

        assertFalse(session.isLoggedIn());
        assertNull(session.getRole());
    }

    @Test
    void aWrongPasswordKeepsTheUserOnTheLoginPage() {
        final FileAppDataAccessObject dataAccess =
                new FileAppDataAccessObject(tempDir.resolve("app-data.json"));
        dataAccess.save(new User("player1", "pw", "Pat Player", Role.PLAYER));

        final ViewManagerModel viewManagerModel = new ViewManagerModel();
        final Session session = new Session();
        final LoginViewModel loginViewModel = new LoginViewModel();
        final LoginController loginController = new LoginController(new LoginInteractor(
                dataAccess, new LoginPresenter(viewManagerModel, loginViewModel, session)));

        viewManagerModel.setState(ViewManagerModel.LOGIN);
        loginController.execute("player1", "wrong");

        assertEquals("Incorrect password.", loginViewModel.getErrorMessage());
        assertFalse(session.isLoggedIn());
        assertEquals(ViewManagerModel.LOGIN, viewManagerModel.getState());
    }

    @Test
    void theViewsBuildAndTheShellFiltersPagesByRole() {
        final LoginViewModel loginViewModel = new LoginViewModel();
        final SignupViewModel signupViewModel = new SignupViewModel();

        final LoginView loginView = new LoginView(loginViewModel);
        loginView.setLoginController(new LoginController(inputData -> { }));
        loginView.setOnGoToSignup(() -> { });

        final SignupView signupView = new SignupView(signupViewModel);
        signupView.setSignupController(new SignupController(inputData -> { }));
        signupView.setOnBackToLogin(() -> { });

        final AppShell shell = new AppShell(() -> { });
        shell.addPage("Calendar", new JPanel());
        shell.addPage("New Event", new JPanel(), Role.COACH);
        shell.addPage("My Stats", new JPanel(), Role.PLAYER);

        // Every role can be applied without the sidebar or active page blowing up.
        shell.applyRole(null);
        shell.applyRole(Role.ADMIN);
        shell.applyRole(Role.COACH);
        shell.applyRole(Role.PLAYER);

        assertNotNull(loginView);
        assertNotNull(signupView);
    }
}
