package use_case;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import entity.Event;
import use_case.create_event.*;

/** Assigned C1's prerequisite: existing create_event happy/failure tests. */
public final class CreateEventAssignedTests {
    private Event saved;
    private String error;
    private final CreateEventDataAccessInterface dataAccess = new CreateEventDataAccessInterface() {
        @Override public boolean teamExists(String teamId) { return "team".equals(teamId); }
        @Override public void saveEvent(Event event) { saved = event; }
    };
    private final CreateEventOutputBoundary output = new CreateEventOutputBoundary() {
        @Override public void prepareSuccessView(CreateEventOutputData data) { error = null; }
        @Override public void prepareFailView(String message) { error = message; }
    };
    @Test void createsValidFuturePractice() {
        new CreateEventInteractor(dataAccess, output).execute(new CreateEventInputData("team", "PRACTICE",
                LocalDateTime.now().plusDays(10), "Gym", null));
        assertNotNull(saved);
        assertEquals("team", saved.getTeamId());
        assertNull(error);
    }
    @Test void rejectsMissingTeam() {
        new CreateEventInteractor(dataAccess, output).execute(new CreateEventInputData("missing", "PRACTICE",
                LocalDateTime.now().plusDays(10), "Gym", null));
        assertNull(saved);
        assertNotNull(error);
    }
    @Test void rejectsPastDate() {
        new CreateEventInteractor(dataAccess, output).execute(new CreateEventInputData("team", "GAME",
                LocalDateTime.now().minusDays(2), "Gym", "Visitors"));
        assertNull(saved);
        assertNotNull(error);
    }
    @Test void rejectsBlankLocation() {
        new CreateEventInteractor(dataAccess, output).execute(new CreateEventInputData("team", "GAME",
                LocalDateTime.now().plusDays(10), " ", "Visitors"));
        assertNull(saved);
        assertNotNull(error);
    }
    @Test void rejectsUnknownEventType() {
        new CreateEventInteractor(dataAccess, output).execute(new CreateEventInputData("team", "INVALID",
                LocalDateTime.now().plusDays(10), "Gym", null));
        assertNull(saved);
        assertNotNull(error);
    }
}
