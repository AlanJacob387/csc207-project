package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import interface_adapter.signup.SignupViewModel;

/**
 * Tests the role picker on the sign-up page: it offers the team roles and never
 * ADMIN, so an account cannot grant itself administration by signing up.
 */
public class SignupViewTest {

    @Test
    void thePickerOffersEveryTeamRoleAndNoAdministrator() {
        final List<String> choices = List.of(SignupView.roleChoices());

        assertTrue(choices.contains("COACH"), choices.toString());
        assertTrue(choices.contains("PLAYER"), choices.toString());
        assertTrue(choices.contains("PARENT"), choices.toString());
        assertFalse(choices.contains("ADMIN"), "sign-up must not offer administration: "
                + choices);
        assertEquals(3, choices.size(), choices.toString());
    }

    @Test
    void thePickerDefaultsToPlayer() {
        final SignupView view = new SignupView(new SignupViewModel());

        assertEquals("PLAYER", view.selectedRoleName());
    }
}
