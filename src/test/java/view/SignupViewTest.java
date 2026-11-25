package view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import interface_adapter.signup.SignupViewModel;

/**
 * Tests the role picker on the sign-up page: the view model offers the roles the
 * rule allows and never ADMIN, and the page shows whatever the model carries
 * rather than deciding for itself.
 */
public class SignupViewTest {

    @Test
    void theViewModelOffersEveryTeamRoleAndNoAdministrator() {
        final List<String> choices = new SignupViewModel().getRoleChoices();

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

    @Test
    void thePickerShowsWhateverTheViewModelOffers() {
        final SignupView view = new SignupView(new SignupViewModel(List.of("COACH")));

        assertEquals("COACH", view.selectedRoleName(),
                "the page must not add roles of its own");
    }
}
