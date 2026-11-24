package use_case.change_password;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import data_access.InMemoryUserDataAccessObject;
import entity.Role;
import entity.User;
import interface_adapter.Session;

/**
 * Tests the Change Password use case: the happy path plus every way it can be
 * refused. The username comes from the session, never from the input data.
 */
class ChangePasswordInteractorTest {

    private InMemoryUserDataAccessObject dataAccess;
    private Session session;
    private RecordingPresenter presenter;
    private ChangePasswordInteractor interactor;

    /** Captures whatever the interactor asks the presenter to show. */
    private static class RecordingPresenter implements ChangePasswordOutputBoundary {

        private ChangePasswordOutputData success;
        private String error;

        @Override
        public void prepareSuccessView(ChangePasswordOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new InMemoryUserDataAccessObject();
        dataAccess.save(new User("coach1", "oldpass1", "Coach One", Role.COACH));

        session = new Session();
        session.setCurrentUser(new User("coach1", "oldpass1", "Coach One", Role.COACH));

        presenter = new RecordingPresenter();
        interactor = new ChangePasswordInteractor(
                dataAccess, presenter, session);
    }

    @Test
    void changesThePasswordAndKeepsTheRestOfTheProfile() {
        interactor.execute(new ChangePasswordInputData("oldpass1", "newpass1", "newpass1"));

        assertNull(presenter.error);
        assertEquals("Password changed.", presenter.success.getMessage());
        assertEquals("coach1", presenter.success.getUsername());
        assertTrue(dataAccess.get("coach1").matches("newpass1"));
        assertEquals("Coach One", dataAccess.get("coach1").getDisplayName());
        assertEquals(Role.COACH, dataAccess.get("coach1").getRole());
    }

    @Test
    void anIncorrectCurrentPasswordFails() {
        interactor.execute(new ChangePasswordInputData("wrong", "newpass1", "newpass1"));

        assertEquals("The current password is incorrect.", presenter.error);
        assertNull(presenter.success);
        assertEquals("oldpass1", dataAccess.get("coach1").getPassword());
    }

    @Test
    void mismatchedNewPasswordsFail() {
        interactor.execute(new ChangePasswordInputData("oldpass1", "newpass1", "different1"));

        assertEquals("The two new passwords do not match.", presenter.error);
        assertEquals("oldpass1", dataAccess.get("coach1").getPassword());
    }

    @Test
    void aNewPasswordShorterThanTheRuleIsRefused() {
        interactor.execute(new ChangePasswordInputData("oldpass1", "pw", "pw"));

        assertEquals("Passwords are 8 to 128 characters.", presenter.error);
        assertEquals("oldpass1", dataAccess.get("coach1").getPassword(),
                "a refused password must leave the stored credential alone");
    }

    @Test
    void reusingTheSamePasswordFails() {
        interactor.execute(new ChangePasswordInputData("oldpass1", "oldpass1", "oldpass1"));

        assertEquals("The new password must be different from the current one.", presenter.error);
        assertEquals("oldpass1", dataAccess.get("coach1").getPassword());
    }

    @Test
    void blankFieldsFail() {
        interactor.execute(new ChangePasswordInputData("", "", ""));

        assertEquals("Fill in all three password fields.", presenter.error);
    }

    @Test
    void aSignedOutUserCannotChangeAPassword() {
        session.clearCurrentUser();

        interactor.execute(new ChangePasswordInputData("oldpass1", "newpass1", "newpass1"));

        assertEquals("You must be signed in to change your password.", presenter.error);
        assertEquals("oldpass1", dataAccess.get("coach1").getPassword());
    }

    @Test
    void aMissingAccountFails() {
        session.setCurrentUser(new User("ghost", "oldpass1", "Ghost", Role.PLAYER));

        interactor.execute(new ChangePasswordInputData("oldpass1", "newpass1", "newpass1"));

        assertEquals("No account found for ghost.", presenter.error);
    }

    @Test
    void aBrokenStoreIsReportedInsteadOfThrown() {
        final ChangePasswordUserDataAccessInterface broken =
                new ChangePasswordUserDataAccessInterface() {
                    @Override
                    public User get(String username) {
                        throw new IllegalStateException("the file is unreadable");
                    }

                    @Override
                    public void save(User user) {
                        throw new IllegalStateException("the file is unreadable");
                    }
                };

        new ChangePasswordInteractor(broken, presenter, session)
                .execute(new ChangePasswordInputData("oldpass1", "newpass1", "newpass1"));

        assertTrue(presenter.error.startsWith("Could not read the account store:"));
        assertNull(presenter.success);
    }
}
