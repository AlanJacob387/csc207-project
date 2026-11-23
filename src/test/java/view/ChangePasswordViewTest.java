package view;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import data_access.FileAppDataAccessObject;
import entity.Role;
import entity.UserFactory;
import interface_adapter.Session;
import interface_adapter.change_password.ChangePasswordController;
import interface_adapter.change_password.ChangePasswordPresenter;
import interface_adapter.change_password.ChangePasswordViewModel;
import use_case.change_password.ChangePasswordInteractor;

/**
 * Regression tests for the Account page.
 *
 * The page is created once and reused across sessions, so it must never carry
 * one user's half-typed password change into the next user's session.
 */
public class ChangePasswordViewTest {

    private static final String PASSWORD = "pw";
    private static final String DRAFT = "alice-draft";

    private final UserFactory userFactory = new UserFactory();

    @Test
    void aDraftFromThePreviousUserIsClearedWhenSomeoneElseSignsIn() {
        final Session session = new Session();
        final ChangePasswordView view =
                new ChangePasswordView(new ChangePasswordViewModel(), session);

        session.setCurrentUser(userFactory.create("alice", PASSWORD, "Alice", Role.COACH));
        view.typePasswords(PASSWORD, DRAFT, DRAFT);

        session.clearCurrentUser();
        session.setCurrentUser(userFactory.create("bob", PASSWORD, "Bob", Role.PLAYER));

        assertArrayEquals(new String[] {"", "", ""}, view.typedPasswords());
    }

    @Test
    void pressingChangeAfterTheUserChangesLeavesTheNewUsersPasswordAlone(@TempDir Path folder) {
        final FileAppDataAccessObject store =
                new FileAppDataAccessObject(folder.resolve("app-data.json"));
        store.save(userFactory.create("alice", PASSWORD, "Alice", Role.COACH));
        store.save(userFactory.create("bob", PASSWORD, "Bob", Role.PLAYER));

        final Session session = new Session();
        final ChangePasswordViewModel viewModel = new ChangePasswordViewModel();
        final ChangePasswordView view = new ChangePasswordView(viewModel, session);
        view.setChangePasswordController(new ChangePasswordController(
                new ChangePasswordInteractor(store, new ChangePasswordPresenter(viewModel),
                        session, userFactory)));

        session.setCurrentUser(userFactory.create("alice", PASSWORD, "Alice", Role.COACH));
        view.typePasswords(PASSWORD, DRAFT, DRAFT);

        session.clearCurrentUser();
        session.setCurrentUser(userFactory.create("bob", PASSWORD, "Bob", Role.PLAYER));

        view.onChange();

        assertEquals(PASSWORD, store.get("bob").getPassword());
        assertEquals("Fill in all three password fields.", viewModel.getErrorMessage());
    }

    @Test
    void feedbackFromThePreviousUserDoesNotSurviveTheSessionChange() {
        final Session session = new Session();
        final ChangePasswordViewModel viewModel = new ChangePasswordViewModel();
        new ChangePasswordView(viewModel, session);
        viewModel.setSuccessMessage("Password changed.");

        session.clearCurrentUser();

        assertEquals("", viewModel.getSuccessMessage());
    }
}
