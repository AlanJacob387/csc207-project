package view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import interface_adapter.login.LoginViewModel;

/**
 * Tests the message line on the sign-in page, which is shared by the login result
 * and by a problem that happened before the page could open.
 */
class LoginViewTest {

    @Test
    void aStartupProblemIsShownWithoutLosingTheStoreLine() {
        final LoginView view = new LoginView(new LoginViewModel());

        view.setStoreDescription("Hosted database: https://example.supabase.co");
        view.setStartupProblem("Could not reach the account store: connection refused.");

        assertEquals("Could not reach the account store: connection refused.",
                view.startupProblem());
        assertEquals("Hosted database: https://example.supabase.co", view.storeDescription());
    }

    @Test
    void aBlankProblemLeavesTheLineEmpty() {
        final LoginView view = new LoginView(new LoginViewModel());
        view.setStartupProblem("something");

        view.setStartupProblem("  ");

        assertEquals(" ", view.startupProblem());
    }
}
