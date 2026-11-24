package use_case.export_schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import entity.Event;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/**
 * Tests the Export Schedule use case: the exact calendar text handed to the
 * file writer, and every way an export can fail or have nothing to do.
 */
class ExportScheduleInteractorTest {

    private static final String TARGET = "C:/exports/team-schedule.ics";

    private FakeExportDataAccess dataAccess;
    private FakeFileWriter fileWriter;
    private FakeCurrentUser currentUser;
    private RecordingPresenter presenter;
    private ExportScheduleInteractor interactor;

    /** Teams and events held in memory, scoped to what this use case reads. */
    private static class FakeExportDataAccess implements ExportScheduleDataAccessInterface {

        private final Map<String, List<String>> teamsByMember = new HashMap<>();
        private final Map<String, String> teamNames = new HashMap<>();
        private final List<Event> events = new ArrayList<>();
        private boolean failReads;
        private boolean failReadsWithIo;

        @Override
        public List<String> getTeamIdsForMember(String username) {
            if (failReads) {
                throw new IllegalStateException("App data file is not valid JSON");
            }
            if (failReadsWithIo) {
                throw new UncheckedIOException("Could not read app data",
                        new IOException("disk unavailable"));
            }
            return new ArrayList<>(teamsByMember.getOrDefault(username, new ArrayList<>()));
        }

        @Override
        public String getTeamName(String teamId) {
            return teamNames.get(teamId);
        }

        @Override
        public List<Event> getEventsForTeams(Collection<String> teamIds) {
            final List<Event> matches = new ArrayList<>();
            for (Event event : events) {
                if (teamIds.contains(event.getTeamId())) {
                    matches.add(event);
                }
            }
            return matches;
        }
    }

    /** Records what would have been written, or fails like a real disk would. */
    private static class FakeFileWriter implements ScheduleFileWriter {

        private String writtenPath;
        private String writtenContent;
        private int calls;
        private String failWith;

        @Override
        public void write(String targetPath, String content) {
            calls++;
            if (failWith != null) {
                throw new ScheduleWriteException(failWith, null);
            }
            this.writtenPath = targetPath;
            this.writtenContent = content;
        }
    }

    /** Stands in for the session; only the username matters here. */
    private static class FakeCurrentUser implements CurrentUserInterface {
        private entity.Role currentRole;

        @Override
        public entity.Role getCurrentRole() {
            return currentRole;
        }


        private String username = "";

        @Override
        public String getCurrentUsername() {
            return username;
        }

        @Override
        public void setCurrentUser(User user) {
            currentRole = user.getRole();
            this.username = user.getUsername();
        }

        @Override
        public void clearCurrentUser() {
            this.username = "";
        }
    }

    /** Captures whatever the interactor asks the presenter to show. */
    private static class RecordingPresenter implements ExportScheduleOutputBoundary {

        private ExportScheduleOutputData success;
        private String nothingToExport;
        private String error;

        @Override
        public void prepareSuccessView(ExportScheduleOutputData outputData) {
            this.success = outputData;
        }

        @Override
        public void prepareNothingToExportView(String message) {
            this.nothingToExport = message;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.error = errorMessage;
        }
    }

    @BeforeEach
    void setUp() {
        dataAccess = new FakeExportDataAccess();
        fileWriter = new FakeFileWriter();
        currentUser = new FakeCurrentUser();
        presenter = new RecordingPresenter();
        interactor = new ExportScheduleInteractor(dataAccess, fileWriter, presenter, currentUser);

        dataAccess.teamsByMember.put("coach1", List.of("hawks"));
        dataAccess.teamNames.put("hawks", "Hawks");
        dataAccess.teamNames.put("owls", "Owls");

        // Added out of order on purpose; the export must come out sorted.
        dataAccess.events.add(new Event("late", "hawks", Event.EventType.PRACTICE,
                LocalDateTime.of(2026, 12, 3, 18, 0), "Main Gym", null));
        final Event cancelled = new Event("cancelled", "hawks", Event.EventType.GAME,
                LocalDateTime.of(2026, 11, 28, 15, 0), "Civic Arena, Hall B", "Celtics");
        cancelled.cancel();
        dataAccess.events.add(cancelled);
        dataAccess.events.add(new Event("early", "hawks", Event.EventType.GAME,
                LocalDateTime.of(2026, 11, 10, 17, 30), "North Court", "Raptors; Jr"));
        dataAccess.events.add(new Event("other-team", "owls", Event.EventType.GAME,
                LocalDateTime.of(2026, 11, 18, 18, 0), "Away Gym", "Wolves"));

        currentUser.username = "coach1";
    }

    @Test
    void writesWholeScheduleAsValidCalendar() {
        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.error);
        assertEquals(TARGET, presenter.success.getTargetPath());
        assertEquals(3, presenter.success.getEventCount());
        assertEquals(0, presenter.success.getSkippedCount());
        assertEquals(TARGET, fileWriter.writtenPath);

        final String ics = fileWriter.writtenContent;
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertTrue(ics.contains("X-WR-CALNAME:Hawks\r\n"));
        assertEquals(3, ics.split("BEGIN:VEVENT", -1).length - 1);
        assertFalse(ics.contains("other-team"), "another team's event leaked into the export");

        // Earliest first, with stable UIDs.
        final int early = ics.indexOf("UID:early@teamsports");
        final int cancelledAt = ics.indexOf("UID:cancelled@teamsports");
        final int late = ics.indexOf("UID:late@teamsports");
        assertTrue(early >= 0 && early < cancelledAt && cancelledAt < late);

        assertTrue(ics.contains("DTSTART:20261110T173000\r\nDTEND:20261110T183000\r\n"));
        assertTrue(ics.contains("SUMMARY:Game vs Raptors\\; Jr\r\n"));
        assertTrue(ics.contains("LOCATION:North Court\r\n"));
        assertTrue(ics.contains("SUMMARY:Practice\r\n"));
    }

    @Test
    void cancelledEventIsMarkedCancelled() {
        interactor.execute(new ExportScheduleInputData(TARGET));

        final String ics = fileWriter.writtenContent;
        final String block = ics.substring(ics.indexOf("UID:cancelled@teamsports"),
                ics.indexOf("END:VEVENT", ics.indexOf("UID:cancelled@teamsports")));
        assertTrue(block.contains("STATUS:CANCELLED\r\n"));
        assertTrue(block.contains("SUMMARY:Cancelled: Game vs Celtics\r\n"));
        assertTrue(block.contains("LOCATION:Civic Arena\\, Hall B\r\n"));

        final String earlyBlock = ics.substring(ics.indexOf("UID:early@teamsports"),
                ics.indexOf("END:VEVENT", ics.indexOf("UID:early@teamsports")));
        assertTrue(earlyBlock.contains("STATUS:CONFIRMED\r\n"));
    }

    @Test
    void userOnSeveralTeamsGetsGeneralCalendarName() {
        dataAccess.teamsByMember.put("coach1", List.of("hawks", "owls"));

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertEquals(4, presenter.success.getEventCount());
        assertTrue(fileWriter.writtenContent.contains("X-WR-CALNAME:Team Sports Schedule\r\n"));
    }

    @Test
    void writeFailureIsReportedNotThrown() {
        fileWriter.failWith = "Can't write to C:\\exports\\team-schedule.ics. "
                + "You may not have permission, or another program has it open.";

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.success);
        assertEquals(1, fileWriter.calls);
        assertTrue(presenter.error.startsWith("Export failed. Can't write to"));
    }

    @Test
    void readFailureIsReportedAndNothingIsWritten() {
        dataAccess.failReads = true;

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.success);
        assertEquals(0, fileWriter.calls);
        assertTrue(presenter.error.startsWith("Could not read the schedule:"));
    }

    @Test
    void ioFailureWhileReadingIsReportedNotThrown() {
        dataAccess.failReadsWithIo = true;

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.success);
        assertEquals(0, fileWriter.calls);
        assertTrue(presenter.error.startsWith("Could not read the schedule:"));
    }

    @Test
    void eventWithNoDateIsLeftOutAndCounted() {
        dataAccess.events.add(new Event("undated", "hawks", Event.EventType.PRACTICE,
                null, "Main Gym", null));

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.error);
        assertEquals(3, presenter.success.getEventCount());
        assertEquals(1, presenter.success.getSkippedCount());
        assertFalse(fileWriter.writtenContent.contains("UID:undated@teamsports"));
        assertEquals(3, fileWriter.writtenContent.split("BEGIN:VEVENT", -1).length - 1);
    }

    @Test
    void onlyUndatedEventsMeansNothingToExport() {
        dataAccess.events.clear();
        dataAccess.events.add(new Event("undated", "hawks", Event.EventType.GAME,
                null, "North Court", "Raptors"));

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.success);
        assertNull(presenter.error);
        assertEquals("None of your events have a date yet, so there is nothing to export.",
                presenter.nothingToExport);
        assertEquals(0, fileWriter.calls);
    }

    @Test
    void failsWhenNobodyIsSignedIn() {
        currentUser.clearCurrentUser();

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.success);
        assertEquals("Sign in to export the schedule.", presenter.error);
        assertEquals(0, fileWriter.calls);
    }

    @Test
    void failsWhenUserIsOnNoTeam() {
        currentUser.username = "parent1";

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.success);
        assertEquals("You're not on any team yet.", presenter.error);
        assertEquals(0, fileWriter.calls);
    }

    @Test
    void failsWhenNoPathGiven() {
        interactor.execute(new ExportScheduleInputData("  "));

        assertNull(presenter.success);
        assertEquals("Choose a file to export to.", presenter.error);
        assertEquals(0, fileWriter.calls);
    }

    @Test
    void teamWithNoEventsIsInformationNotErrorAndWritesNothing() {
        dataAccess.events.clear();

        interactor.execute(new ExportScheduleInputData(TARGET));

        assertNull(presenter.success);
        assertNull(presenter.error);
        assertEquals("There are no events to export yet.", presenter.nothingToExport);
        assertEquals(0, fileWriter.calls);
    }
}
