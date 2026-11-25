package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Role;
import entity.User;
import interface_adapter.Session;
import interface_adapter.manage_users.ChangeUserRoleController;
import interface_adapter.manage_users.CreateUserController;
import interface_adapter.manage_users.DeactivateUserController;
import interface_adapter.manage_users.ListAccountsController;
import interface_adapter.manage_users.ManageUsersViewModel;
import interface_adapter.manage_users.ReactivateUserController;
import use_case.manage_users.ChangeUserRoleInputBoundary;
import use_case.manage_users.CreateUserInputBoundary;
import use_case.manage_users.DeactivateUserInputBoundary;
import use_case.manage_users.ListAccountsInputBoundary;
import use_case.manage_users.ManageUsersOutputData;
import use_case.manage_users.ReactivateUserInputBoundary;

/**
 * Tests the administrator page: what it passes to the three actions, what it
 * lists, and what it shows after a refusal or a success. The three use cases are
 * recording fakes here, so the page is tested on its own.
 */
class ManageUsersViewTest {

    private final List<String> calls = new ArrayList<>();
    private Session session;
    private ManageUsersViewModel viewModel;
    private ManageUsersView view;

    @BeforeEach
    void setUp() {
        final CreateUserInputBoundary create = inputData -> calls.add("create:"
                + inputData.getUsername() + "|" + inputData.getDisplayName() + "|"
                + inputData.getPassword() + "|" + inputData.getRoleName());
        final ChangeUserRoleInputBoundary change = inputData -> calls.add("role:"
                + inputData.getUsername() + "|" + inputData.getRoleName());
        final DeactivateUserInputBoundary deactivate = inputData ->
                calls.add("deactivate:" + inputData.getUsername());
        final ReactivateUserInputBoundary reactivate = inputData ->
                calls.add("reactivate:" + inputData.getUsername());
        final ListAccountsInputBoundary list = () -> calls.add("list");

        session = new Session();
        viewModel = new ManageUsersViewModel();
        view = new ManageUsersView(
                new ListAccountsController(list),
                new CreateUserController(create),
                new ChangeUserRoleController(change),
                new DeactivateUserController(deactivate),
                new ReactivateUserController(reactivate),
                viewModel, session);
    }

    @Test
    void refreshAsksForTheAccountList() {
        view.clickRefresh();

        assertEquals(List.of("list"), calls);
    }

    @Test
    void signingInAsAnAdministratorLoadsTheList() {
        session.setCurrentUser(new User("admin1", "pw", "Ada Admin", Role.ADMIN));

        assertEquals(List.of("list"), calls);
    }

    @Test
    void signingInAsAnyoneElseEmptiesThePageAndLeavesTheDecisionToTheUseCase() {
        showAccounts(new User("admin1", "pw", "Ada Admin", Role.ADMIN));

        session.setCurrentUser(new User("player1", "pw", "Pat Player", Role.PLAYER));

        assertEquals(List.of("list"), calls,
                "the page asks for the list; whether this caller may read it is the "
                        + "use case's decision, not the view's");
        assertTrue(view.listedAccounts().contains("No accounts"), view.listedAccounts());
    }

    @Test
    void creatingAnAccountPassesEveryTypedValue() {
        view.typeNewAccount("player9", "Pat Nine", "secret", "secret");
        view.selectNewRole("COACH");

        view.clickCreate();

        assertEquals(List.of("create:player9|Pat Nine|secret|COACH"), calls);
    }

    @Test
    void changingARolePassesTheSelectedAccountAndRole() {
        showAccounts(new User("player1", "pw", "Pat Player", Role.PLAYER));
        view.selectAccount("player1");
        view.selectRole("COACH");

        view.clickChangeRole();

        assertEquals(List.of("role:player1|COACH"), calls);
    }

    @Test
    void deactivatingPassesTheSelectedAccount() {
        showAccounts(new User("player1", "pw", "Pat Player", Role.PLAYER));
        view.selectAccount("player1");

        view.clickDeactivate();

        assertEquals(List.of("deactivate:player1"), calls);
    }

    @Test
    void reactivatingPassesTheSelectedAccount() {
        showAccounts(new User("player1", "pw", "Pat Player", Role.PLAYER, false));
        view.selectAccount("player1");

        view.clickReactivate();

        assertEquals(List.of("reactivate:player1"), calls);
    }

    @Test
    void theListShowsEveryAccountWithItsRoleAndStatus() {
        showAccounts(new User("admin1", "pw", "Ada Admin", Role.ADMIN),
                new User("player1", "pw", "Pat Player", Role.PLAYER, false));

        final String listed = view.listedAccounts();

        assertTrue(listed.contains("admin1 - Ada Admin - ADMIN - active"), listed);
        assertTrue(listed.contains("player1 - Pat Player - PLAYER - deactivated"), listed);
    }

    @Test
    void aRefusalIsShownAndKeepsTheListVisible() {
        showAccounts(new User("player1", "pw", "Pat Player", Role.PLAYER));

        viewModel.showError("You cannot change your own role.");

        assertEquals("You cannot change your own role.", view.status());
        assertTrue(view.listedAccounts().contains("player1"), "the list should stay on screen");
    }

    @Test
    void aSuccessClearsTheCreateForm() {
        viewModel.showAccounts(ManageUsersOutputData.of("Account created for player9.",
                List.of(new User("player9", "pw", "Pat Nine", Role.PLAYER))));

        assertTrue(view.status().contains("Account created for player9"));
        view.clickCreate();

        assertEquals(List.of("create:|||PLAYER"), calls,
                "after a success the form should be empty again, on the safe role");
    }

    @Test
    void theChangeRolePickerStartsOnTheSelectedAccountsRole() {
        showAccounts(new User("admin1", "pw", "Ada Admin", Role.ADMIN),
                new User("player1", "pw", "Pat Player", Role.PLAYER));
        view.selectAccount("player1");

        view.clickChangeRole();

        assertEquals(List.of("role:player1|PLAYER"), calls,
                "pressing Change role must not assign a different role by itself");
    }

    @Test
    void aDeliberateRoleChoiceSurvivesAnUnrelatedMessage() {
        showAccounts(new User("player1", "pw", "Pat Player", Role.PLAYER));
        view.selectAccount("player1");
        view.selectRole("COACH");

        viewModel.showError("Something else went wrong.");
        view.clickChangeRole();

        assertEquals(List.of("role:player1|COACH"), calls,
                "the administrator's own choice must not be reset by a refresh");
    }

    @Test
    void choosingAnotherAccountMovesThePickerToThatAccountsRole() {
        showAccounts(new User("admin1", "pw", "Ada Admin", Role.ADMIN),
                new User("player1", "pw", "Pat Player", Role.PLAYER));
        view.selectAccount("player1");
        view.selectRole("PARENT");

        view.selectAccount("admin1");
        view.clickChangeRole();

        assertEquals(List.of("role:admin1|ADMIN"), calls,
                "switching accounts shows that account's own role, with no extra refresh");
    }

    @Test
    void theFirstAccountIsSelectedSoAnActionNeedsNoExtraClick() {
        showAccounts(new User("admin1", "pw", "Ada Admin", Role.ADMIN),
                new User("player1", "pw", "Pat Player", Role.PLAYER));

        assertEquals("admin1", view.selectedAccount());
    }

    private void showAccounts(User... users) {
        viewModel.showAccounts(ManageUsersOutputData.of("Loaded.", List.of(users)));
    }
}
