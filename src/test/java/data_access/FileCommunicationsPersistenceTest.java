package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import entity.AbsenceNotice;
import entity.Announcement;
import entity.Event;
import entity.EventNotification;
import entity.ParentPlayerLink;
import entity.Team;

/** Exercises event changes and family communication against temporary real JSON stores. */
class FileCommunicationsPersistenceTest {
    private static final LocalDateTime WHEN = LocalDateTime.of(2026, 12, 2, 18, 30);
    @TempDir
    Path tempDir;

    @Test
    void eventCreationEditingAndCancellationEachWriteOneDurableNotice() {
        final Path file = file();
        final FileAppDataAccessObject dao = initialStore(file);
        dao.saveEvent(game("event-1", "Original Arena", false));
        dao.saveEvent(game("event-1", "Original Arena", false));
        assertEquals(List.of("CREATED"), changeTypes(dao));

        dao.updateEvent(game("event-1", "New Arena", false));
        dao.updateEvent(game("event-1", "New Arena", false));
        assertEquals(List.of("CREATED", "CHANGED"), changeTypes(dao));

        dao.updateEvent(game("event-1", "New Arena", true));
        dao.updateEvent(game("event-1", "New Arena", true));
        assertEquals(List.of("CREATED", "CHANGED", "CANCELLED"), changeTypes(dao));
        final FileAppDataAccessObject reopened = new FileAppDataAccessObject(file);
        assertTrue(reopened.getEvent("event-1").isCancelled());
        assertEquals(changeTypes(dao), changeTypes(reopened));
        assertEquals(3, reopened.getEventNotifications().stream()
                .map(EventNotification::getNotificationId).distinct().count());
        assertTrue(reopened.getEventNotifications().get(0).getMessage().contains("Original Arena"));
        assertTrue(reopened.getEventNotifications().get(1).getMessage().contains("New Arena"));
    }

    @Test
    void oldStoredEventsDoNotInventHistoricalNotificationsOnLoadOrAnUnchangedSave() throws IOException {
        final JSONObject original = legacyDocument();
        Files.writeString(file(), original.toString(2));
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file());

        assertTrue(dao.getEventNotifications().isEmpty());
        dao.saveEvent(game("event-1", "Original Arena", false));
        assertTrue(dao.getEventNotifications().isEmpty());

        dao.updateEvent(game("event-1", "New Arena", false));
        assertEquals(List.of("CHANGED"), changeTypes(new FileAppDataAccessObject(file())));
    }

    @Test
    void aFailedCreationLeavesNeitherAnEventNorANoticeAndARetryCreatesOne() throws IOException {
        initialStore(file());
        final String before = Files.readString(file());
        final FailingStore dao = new FailingStore(file());

        assertThrows(UncheckedIOException.class, () -> dao.saveEvent(game("event-1", "Arena", false)));

        assertNull(dao.getEvent("event-1"));
        assertTrue(dao.getEventNotifications().isEmpty());
        assertEquals(before, Files.readString(file()));
        assertNoTemporaryFiles();
        dao.failReplacement = false;
        dao.saveEvent(game("event-1", "Arena", false));
        assertEquals(List.of("CREATED"), changeTypes(new FileAppDataAccessObject(file())));
    }

    @Test
    void aFailedEditKeepsBothTheOriginalEventAndItsHistory() throws IOException {
        final FileAppDataAccessObject initial = initialStore(file());
        initial.saveEvent(game("event-1", "Original Arena", false));
        final String before = Files.readString(file());
        final FailingStore dao = new FailingStore(file());

        assertThrows(UncheckedIOException.class, () -> dao.updateEvent(game("event-1", "New Arena", false)));

        assertEquals("Original Arena", dao.getEvent("event-1").getLocation());
        assertEquals(List.of("CREATED"), changeTypes(dao));
        assertEquals(before, Files.readString(file()));
        assertNoTemporaryFiles();
        dao.failReplacement = false;
        dao.updateEvent(game("event-1", "New Arena", false));
        dao.updateEvent(game("event-1", "New Arena", false));
        assertEquals(List.of("CREATED", "CHANGED"), changeTypes(new FileAppDataAccessObject(file())));
    }

    @Test
    void aFailedCancellationLeavesNoCancellationNoticeAndARetryPublishesItOnce() throws IOException {
        final FileAppDataAccessObject initial = initialStore(file());
        initial.saveEvent(game("event-1", "Arena", false));
        final String before = Files.readString(file());
        final FailingStore dao = new FailingStore(file());

        assertThrows(UncheckedIOException.class, () -> dao.updateEvent(game("event-1", "Arena", true)));

        assertFalse(dao.getEvent("event-1").isCancelled());
        assertEquals(List.of("CREATED"), changeTypes(dao));
        assertEquals(before, Files.readString(file()));
        dao.failReplacement = false;
        dao.updateEvent(game("event-1", "Arena", true));
        dao.updateEvent(game("event-1", "Arena", true));
        assertEquals(List.of("CREATED", "CANCELLED"), changeTypes(new FileAppDataAccessObject(file())));
    }

    @Test
    void anInvalidEventCannotStrandAnUnsavedRecordBeforeNotificationCreation() throws IOException {
        final FileAppDataAccessObject dao = initialStore(file());
        final String before = Files.readString(file());
        final Event invalid = new Event("invalid", "team-1", null, WHEN, "Arena", null);

        assertThrows(RuntimeException.class, () -> dao.saveEvent(invalid));

        assertNull(dao.getEvent("invalid"));
        assertTrue(dao.getEventNotifications().isEmpty());
        assertEquals(before, Files.readString(file()));
    }

    @Test
    void callerMutationsDoNotChangeTheStoredEventUntilAChangeAndNoticeAreSavedTogether() {
        final FileAppDataAccessObject dao = initialStore(file());
        final Event original = game("event-1", "Original Arena", false);
        dao.saveEvent(original);
        original.setLocation("Unsaved caller change");
        assertEquals("Original Arena", dao.getEvent("event-1").getLocation());
        assertEquals(List.of("CREATED"), changeTypes(dao));

        final Event detached = dao.getEvent("event-1");
        detached.setLocation("New Arena");
        assertEquals("Original Arena", dao.getEvent("event-1").getLocation());
        dao.updateEvent(detached);
        detached.cancel();

        assertEquals("New Arena", dao.getEvent("event-1").getLocation());
        assertFalse(dao.getEvent("event-1").isCancelled());
        assertEquals(List.of("CREATED", "CHANGED"), changeTypes(new FileAppDataAccessObject(file())));
    }

    @Test
    void familyLinksSurviveRestartAndRevokingOnePreservesOtherChildrenAndParents() {
        final FileAppDataAccessObject dao = initialStore(file());
        dao.saveParentPlayerLink(new ParentPlayerLink("parent-a", "child-a"));
        dao.saveParentPlayerLink(new ParentPlayerLink("parent-a", "child-b"));
        dao.saveParentPlayerLink(new ParentPlayerLink("parent-b", "child-a"));
        dao.saveParentPlayerLink(new ParentPlayerLink("parent-a", "child-a"));
        assertEquals(3, dao.getParentPlayerLinks().size());

        dao.deleteParentPlayerLink("parent-a", "child-a");

        final FileAppDataAccessObject reopened = new FileAppDataAccessObject(file());
        assertEquals(List.of("child-b"), reopened.getLinkedPlayerUsernames("parent-a"));
        assertEquals(List.of("child-a"), reopened.getLinkedPlayerUsernames("parent-b"));
        assertTrue(reopened.getLinkedPlayerUsernames("unknown-parent").isEmpty());
    }

    @Test
    void distinctFamilyPairsDoNotCollideWhenUsernamesContainSeparators() {
        final FileAppDataAccessObject dao = initialStore(file());
        dao.saveParentPlayerLink(new ParentPlayerLink("parent|child", "a"));
        dao.saveParentPlayerLink(new ParentPlayerLink("parent", "child|a"));

        final FileAppDataAccessObject reopened = new FileAppDataAccessObject(file());

        assertEquals(List.of("a"), reopened.getLinkedPlayerUsernames("parent|child"));
        assertEquals(List.of("child|a"), reopened.getLinkedPlayerUsernames("parent"));
    }

    @Test
    void failedLinkCreationAndRevocationKeepThePreviousRelationships() throws IOException {
        final FileAppDataAccessObject initial = initialStore(file());
        initial.saveParentPlayerLink(new ParentPlayerLink("parent-a", "child-a"));
        final String before = Files.readString(file());
        final FailingStore dao = new FailingStore(file());

        assertThrows(UncheckedIOException.class, () -> dao.saveParentPlayerLink(
                new ParentPlayerLink("parent-b", "child-b")));
        assertThrows(UncheckedIOException.class, () -> dao.deleteParentPlayerLink("parent-a", "child-a"));

        assertEquals(List.of("child-a"), dao.getLinkedPlayerUsernames("parent-a"));
        assertTrue(dao.getLinkedPlayerUsernames("parent-b").isEmpty());
        assertEquals(before, Files.readString(file()));
        assertNoTemporaryFiles();
    }

    @Test
    void eachParentCanKeepTheirOwnNoticeForTheSameChildAndEvent() {
        final FileAppDataAccessObject dao = initialStore(file());
        dao.saveAbsenceNotice(notice("notice-a", "parent-a", "child-a", "event-1", "At an appointment"));
        dao.saveAbsenceNotice(notice("notice-b", "parent-b", "child-a", "event-1", "Travelling"));
        dao.saveAbsenceNotice(notice("notice-a", "parent-a", "child-a", "event-1", "Appointment moved"));

        final FileAppDataAccessObject reopened = new FileAppDataAccessObject(file());

        assertEquals(2, reopened.getAbsenceNotices().size());
        assertEquals("Appointment moved", reopened.getAbsenceNotices().get(0).getReason());
        assertEquals("Travelling", reopened.getAbsenceNotices().get(1).getReason());
        assertEquals(WHEN.minusDays(1), reopened.getAbsenceNotices().get(0).getSubmittedAt());
    }

    @Test
    void noticeIdCollisionsCannotOverwriteADifferentParentChildOrEvent() throws IOException {
        final FileAppDataAccessObject dao = initialStore(file());
        dao.saveAbsenceNotice(notice("notice-a", "parent-a", "child-a", "event-1", "Original"));
        final String before = Files.readString(file());

        for (AbsenceNotice collision : List.of(
                notice("notice-a", "parent-b", "child-a", "event-1", "Other parent"),
                notice("notice-a", "parent-a", "child-b", "event-1", "Other child"),
                notice("notice-a", "parent-a", "child-a", "event-2", "Other event"),
                notice("different-id", "parent-a", "child-a", "event-1", "Same absence twice"))) {
            assertThrows(IllegalStateException.class, () -> dao.saveAbsenceNotice(collision));
        }

        assertEquals(before, Files.readString(file()));
        assertEquals(1, dao.getAbsenceNotices().size());
        assertEquals("Original", dao.getAbsenceNotices().get(0).getReason());
    }

    @Test
    void failedNoticeCreationOrEditingKeepsTheOriginalDataAndAllowsARetry() throws IOException {
        final FileAppDataAccessObject initial = initialStore(file());
        initial.saveAbsenceNotice(notice("notice-a", "parent-a", "child-a", "event-1", "Original"));
        final String before = Files.readString(file());
        final FailingStore dao = new FailingStore(file());

        assertThrows(UncheckedIOException.class, () -> dao.saveAbsenceNotice(
                notice("notice-a", "parent-a", "child-a", "event-1", "Unsaved change")));
        assertThrows(UncheckedIOException.class, () -> dao.saveAbsenceNotice(
                notice("notice-b", "parent-b", "child-b", "event-1", "Unsaved notice")));

        assertEquals(1, dao.getAbsenceNotices().size());
        assertEquals("Original", dao.getAbsenceNotices().get(0).getReason());
        assertEquals(before, Files.readString(file()));
        assertNoTemporaryFiles();
        dao.failReplacement = false;
        dao.saveAbsenceNotice(notice("notice-a", "parent-a", "child-a", "event-1", "Saved change"));
        assertEquals("Saved change", new FileAppDataAccessObject(file()).getAbsenceNotices().get(0).getReason());
    }

    @Test
    void communicationWritesPreserveLegacyAndUnknownFieldsAcrossAllStoredSections() throws IOException {
        final JSONObject original = legacyDocument();
        original.put("parentLinks", new JSONArray().put(new JSONObject()
                .put("parentUsername", "parent-a").put("playerUsername", "child-a").put("approvedBy", "admin")));
        original.put("absenceNotices", new JSONArray().put(EntityJsonMapper.toJson(
                notice("notice-a", "parent-a", "child-a", "event-1", "Original reason")).put("reviewed", true)));
        original.put("eventNotifications", new JSONArray().put(EntityJsonMapper.toJson(new EventNotification(
                "notification-a", "event-1", "team-1", "CREATED", "Original event", WHEN)).put("channel", "in-app")));
        Files.writeString(file(), original.toString(2));
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file());

        dao.saveAnnouncement(new Announcement("post-1", "team-1", "coach", "Bring water", WHEN));

        final JSONObject after = new JSONObject(Files.readString(file()));
        for (String section : List.of("users", "teams", "events", "attendance", "parentLinks", "absenceNotices",
                "eventNotifications", "unrelatedData")) {
            assertTrue(original.getJSONArray(section).similar(after.getJSONArray(section)), section);
        }
        assertTrue(original.getJSONObject("settings").similar(after.getJSONObject("settings")));
        assertEquals(1, new FileAppDataAccessObject(file()).getEventNotifications().size());
    }

    @Test
    void malformedCommunicationSectionsAndRecordsAreRejectedWithoutChangingTheirSource() throws IOException {
        for (String section : List.of("parentLinks", "absenceNotices", "eventNotifications")) {
            final JSONObject original = legacyDocument().put(section, new JSONObject().put("wrong", true));
            Files.writeString(file(), original.toString(2));
            final String before = Files.readString(file());

            assertThrows(IllegalStateException.class, () -> new FileAppDataAccessObject(file()), section);
            assertEquals(before, Files.readString(file()));
        }
        final JSONObject invalidNotification = new JSONObject().put("notificationId", "bad").put("eventId", "event-1")
                .put("teamId", "team-1").put("changeType", "UNKNOWN").put("message", "Invalid change")
                .put("createdAt", WHEN.toString());
        Files.writeString(file(), legacyDocument().put("eventNotifications", new JSONArray()
                .put(invalidNotification)).toString(2));
        final String before = Files.readString(file());

        assertThrows(IllegalStateException.class, () -> new FileAppDataAccessObject(file()));
        assertEquals(before, Files.readString(file()));
    }

    @Test
    void duplicateAbsenceTuplesInTheStoredFileAreRejectedRatherThanChoosingAnArbitraryNotice() throws IOException {
        final JSONObject original = legacyDocument().put("absenceNotices", new JSONArray()
                .put(EntityJsonMapper.toJson(notice("notice-a", "parent-a", "child-a", "event-1", "First")))
                .put(EntityJsonMapper.toJson(notice("notice-b", "parent-a", "child-a", "event-1", "Second"))));
        Files.writeString(file(), original.toString(2));
        final String before = Files.readString(file());

        assertThrows(IllegalStateException.class, () -> new FileAppDataAccessObject(file()));
        assertEquals(before, Files.readString(file()));
    }

    private FileAppDataAccessObject initialStore(Path file) {
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
        dao.saveTeam(new Team("team-1", "Falcons", "Soccer", "coach"));
        return dao;
    }

    private Event game(String id, String location, boolean cancelled) {
        final Event event = new Event(id, "team-1", Event.EventType.GAME, WHEN, location, "Rivals");
        if (cancelled) {
            event.cancel();
        }
        return event;
    }

    private AbsenceNotice notice(String id, String parent, String player, String event, String reason) {
        return new AbsenceNotice(id, parent, player, event, reason, WHEN.minusDays(1));
    }

    private List<String> changeTypes(FileAppDataAccessObject dao) {
        return dao.getEventNotifications().stream().map(EventNotification::getChangeType).toList();
    }

    private Path file() {
        return tempDir.resolve("app-data.json");
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (Stream<Path> files = Files.list(tempDir)) {
            assertEquals(List.of(file()), files.toList());
        }
    }

    private JSONObject legacyDocument() {
        return new JSONObject("""
                {
                  "users": [{"username":"child-a", "password":"original-password",
                    "displayName":"Original Player", "role":"PLAYER", "jersey":"green"}],
                  "teams": [{"teamId":"team-1", "name":"Falcons", "sport":"Soccer",
                    "coachUsername":"coach", "playerUsernames":["child-a"], "colour":"green"}],
                  "events": [{"eventId":"event-1", "teamId":"team-1", "type":"GAME",
                    "dateTime":"2026-12-02T18:30", "location":"Original Arena", "opponent":"Rivals",
                    "cancelled":false, "venue":{"latitude":43.66}}],
                  "attendance": [{"eventId":"event-1", "playerUsername":"child-a", "status":"PRESENT",
                    "statsNotes":"2 goals", "verified":true}],
                  "unrelatedData": [{"anything":"keep this"}],
                  "settings": {"season":"fall", "archived":false}
                }
                """);
    }

    /** Simulates a failed final replacement while leaving the original file intact. */
    private static class FailingStore extends FileAppDataAccessObject {
        private boolean failReplacement = true;

        FailingStore(Path file) {
            super(file);
        }

        @Override
        void replaceFile(Path temporary, Path target) throws IOException {
            if (failReplacement) {
                throw new IOException("The application store could not be replaced");
            }
            super.replaceFile(temporary, target);
        }
    }
}
