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
import interface_adapter.manage_users.ManageUsersController;
import interface_adapter.manage_users.ManageUsersViewModel;
import use_case.manage_users.ChangeUserRoleInputBoundary;
import use_case.manage_users.CreateUserInputBoundary;
import use_case.manage_users.DeactivateUserInputBoundary;
import use_case.manage_users.ListAccountsInputBoundary;
import use_case.manage_users.ManageUsersOutputData;

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
        final ListAccountsInputBoundary list = () -> calls.add("list");

        session = new Session();
        viewModel = new ManageUsersViewModel();
        view = new ManageUsersView(
                new ManageUsersController(create, change, deactivate, list), viewModel, session);
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
    void signingInAsAnyoneElseEmptiesThePageWithoutAsking() {
        showAccounts(new User("admin1", "pw", "Ada Admin", Role.ADMIN));

        session.setCurrentUser(new User("player1", "pw", "Pat Player", Role.PLAYER));

        assertTrue(calls.isEmpty(), "a non-administrator should not trigger a read");
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

        assertEquals(List.of("create:|||COACH"), calls,
                "after a success the form should be empty again");
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
