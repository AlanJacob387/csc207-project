package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import entity.Announcement;
import entity.Role;
import entity.User;

/** Verifies announcement additions preserve the shared application's existing data. */
class FileAnnouncementPersistenceTest {
    @TempDir
    Path tempDir;

    @Test
    void anOlderStoreKeepsEveryLegacyRecordWhenAnAnnouncementIsAdded() throws IOException {
        final JSONObject original = legacyDocument();
        final Path file = writeDocument(original);
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);

        assertTrue(dao.getAnnouncementsForTeam("team-7").isEmpty());
        dao.saveAnnouncement(announcement("post-1", "team-7", "Bring water"));

        final JSONObject stored = readDocument(file);
        for (String section : new String[]{"users", "teams", "events", "attendance", "parentLinks"}) {
            assertTrue(original.getJSONArray(section).similar(stored.getJSONArray(section)), section);
        }
        assertTrue(original.getJSONObject("settings").similar(stored.getJSONObject("settings")));

        final FileAppDataAccessObject reopened = new FileAppDataAccessObject(file);
        assertEquals("original-password", reopened.get("player-7").getPassword());
        assertEquals("Original Team", reopened.getTeam("team-7").getName());
        assertEquals("Original Field", reopened.getEvent("event-7").getLocation());
        assertEquals("2 goals", reopened.getAttendanceFor("event-7").get(0).getStatsNotes());
        assertEquals("Bring water", reopened.getAnnouncementsForTeam("team-7").get(0).getMessage());
    }

    @Test
    void aLegacySaveKeepsAnnouncementsAndUnknownSectionsAndRecordFields() throws IOException {
        final JSONObject original = legacyDocument();
        original.put("announcements", new JSONArray().put(EntityJsonMapper.toJson(
                announcement("post-1", "team-7", "Original message")).put("delivery", "in-app")));
        final Path file = writeDocument(original);
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);

        dao.saveAnnouncement(announcement("post-2", "team-7", "Second message"));
        dao.save(new User("new-player", "new-password", "New Player", Role.PLAYER));
        dao.save(dao.get("player-7").withRole(Role.COACH));

        final JSONObject stored = readDocument(file);
        assertTrue(original.getJSONArray("parentLinks").similar(stored.getJSONArray("parentLinks")));
        assertTrue(original.getJSONObject("settings").similar(stored.getJSONObject("settings")));
        assertEquals("original-jersey", stored.getJSONArray("users").getJSONObject(0).getString("jersey"));
        assertEquals("COACH", stored.getJSONArray("users").getJSONObject(0).getString("role"));
        assertEquals("in-app", stored.getJSONArray("announcements").getJSONObject(0).getString("delivery"));
        assertEquals(List.of("post-1", "post-2"), new FileAppDataAccessObject(file)
                .getAnnouncementsForTeam("team-7").stream().map(Announcement::getAnnouncementId).toList());
    }

    @Test
    void announcementsAreFilteredByTeamAndReturnedListsCanBeChangedSafely() {
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(tempDir.resolve("app-data.json"));
        dao.saveAnnouncement(announcement("post-1", "team-7", "First team"));
        dao.saveAnnouncement(announcement("post-2", "team-8", "Second team"));

        final List<Announcement> firstTeam = dao.getAnnouncementsForTeam("team-7");
        assertEquals(List.of("post-1"), firstTeam.stream().map(Announcement::getAnnouncementId).toList());
        firstTeam.clear();

        assertEquals(1, dao.getAnnouncementsForTeam("team-7").size());
        assertTrue(dao.getAnnouncementsForTeam("unknown-team").isEmpty());
    }

    @Test
    void anExistingAnnouncementCannotBeOverwritten() throws IOException {
        final Path file = writeDocument(legacyDocument());
        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
        dao.saveAnnouncement(announcement("post-1", "team-7", "Keep this message"));
        final String before = Files.readString(file);

        assertThrows(IllegalStateException.class, () -> dao.saveAnnouncement(
                announcement("post-1", "team-8", "Replacement message")));

        assertEquals(before, Files.readString(file));
        assertEquals("Keep this message", dao.getAnnouncementsForTeam("team-7").get(0).getMessage());
        assertTrue(dao.getAnnouncementsForTeam("team-8").isEmpty());
    }

    @Test
    void aFailedReplacementPreservesTheOldFileAndDoesNotPublishThePost() throws IOException {
        final Path file = writeDocument(legacyDocument());
        final FileAppDataAccessObject initial = new FileAppDataAccessObject(file);
        initial.saveAnnouncement(announcement("post-1", "team-7", "Already stored"));
        final String before = Files.readString(file);
        final FailingReplacementStore dao = new FailingReplacementStore(file);

        assertThrows(UncheckedIOException.class, () -> dao.saveAnnouncement(
                announcement("post-2", "team-7", "Not yet stored")));

        assertEquals(before, Files.readString(file));
        assertEquals(1, dao.getAnnouncementsForTeam("team-7").size());
        assertEquals(1, new FileAppDataAccessObject(file).getAnnouncementsForTeam("team-7").size());
        try (Stream<Path> files = Files.list(tempDir)) {
            assertEquals(List.of(file), files.toList());
        }

        dao.failReplacement = false;
        dao.saveAnnouncement(announcement("post-2", "team-7", "Now stored"));
        assertEquals(2, new FileAppDataAccessObject(file).getAnnouncementsForTeam("team-7").size());
    }

    @Test
    void aFailedFirstWriteDoesNotCreateAnAnnouncementOrLeaveATemporaryFile() throws IOException {
        final Path file = tempDir.resolve("nested").resolve("app-data.json");
        final FailingReplacementStore dao = new FailingReplacementStore(file);

        assertThrows(UncheckedIOException.class, () -> dao.saveAnnouncement(
                announcement("post-1", "team-7", "Not stored")));

        assertFalse(Files.exists(file));
        assertTrue(dao.getAnnouncementsForTeam("team-7").isEmpty());
        try (Stream<Path> files = Files.list(file.getParent())) {
            assertEquals(0, files.count());
        }
    }

    @Test
    void presentSectionsMustBeArraysAndTheDamagedFileIsKeptAside() throws IOException {
        for (String section : new String[]{"users", "teams", "events", "attendance", "announcements"}) {
            for (Object wrongType : new Object[]{new JSONObject().put("unexpected", true), JSONObject.NULL}) {
                final JSONObject invalid = legacyDocument().put(section, wrongType);
                final Path file = writeDocument(invalid);
                final String before = Files.readString(file);

                final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
                assertFalse(Files.exists(file), section + " must be set aside, not rewritten");
                assertEquals(before, Files.readString(dao.quarantinedFile()),
                        section + ": the original bytes must survive");
                assertTrue(dao.get("player-7") == null,
                        section + ": nothing may be guessed from a damaged file");
            }
        }
    }

    @Test
    void malformedAnnouncementRecordsAreSetAsideWithoutBeingGuessedAt() throws IOException {
        final JSONObject missingMessage = EntityJsonMapper.toJson(announcement("post-1", "team-7", "Message"));
        missingMessage.remove("message");
        final JSONObject invalidDate = EntityJsonMapper.toJson(announcement("post-1", "team-7", "Message"));
        invalidDate.put("createdAt", "tomorrow");
        for (Object record : new Object[]{missingMessage, invalidDate, "not an announcement"}) {
            final Path file = writeDocument(legacyDocument().put("announcements", new JSONArray().put(record)));
            final String before = Files.readString(file);

            final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);
            assertEquals(before, Files.readString(dao.quarantinedFile()));
            assertTrue(dao.getAnnouncementsForTeam("team-7").isEmpty());
        }
    }

    @Test
    void duplicateAnnouncementIdsAreSetAsideWithoutChoosingAWinner() throws IOException {
        final JSONArray posts = new JSONArray()
                .put(EntityJsonMapper.toJson(announcement("post-1", "team-7", "First")))
                .put(EntityJsonMapper.toJson(announcement("post-1", "team-8", "Second")));
        final Path file = writeDocument(legacyDocument().put("announcements", posts));
        final String before = Files.readString(file);

        final FileAppDataAccessObject dao = new FileAppDataAccessObject(file);

        assertEquals(before, Files.readString(dao.quarantinedFile()));
        assertTrue(dao.getAnnouncementsForTeam("team-7").isEmpty(),
                "a duplicate must not be resolved by choosing a winner");
        assertTrue(dao.getAnnouncementsForTeam("team-8").isEmpty());
    }

    private Announcement announcement(String id, String teamId, String message) {
        return new Announcement(id, teamId, "coach-7", message, LocalDateTime.of(2026, 11, 9, 18, 0));
    }

    private Path writeDocument(JSONObject document) throws IOException {
        final Path file = tempDir.resolve("app-data.json");
        Files.writeString(file, document.toString(2));
        return file;
    }

    private JSONObject readDocument(Path file) throws IOException {
        return new JSONObject(Files.readString(file));
    }

    private JSONObject legacyDocument() {
        return new JSONObject("""
                {
                  "users": [{"username":"player-7", "password":"original-password",
                    "displayName":"Original Player", "role":"PLAYER", "jersey":"original-jersey"}],
                  "teams": [{"teamId":"team-7", "name":"Original Team", "sport":"Soccer",
                    "coachUsername":"coach-7", "playerUsernames":["player-7"], "colour":"green"}],
                  "events": [{"eventId":"event-7", "teamId":"team-7", "type":"GAME",
                    "dateTime":"2026-11-12T18:00", "location":"Original Field", "opponent":"Bears",
                    "cancelled":false, "venue":{"latitude":43.66}}],
                  "attendance": [{"eventId":"event-7", "playerUsername":"player-7", "status":"PRESENT",
                    "statsNotes":"2 goals", "verified":true}],
                  "parentLinks": [{"parentUsername":"parent-7", "playerUsername":"player-7"}],
                  "settings": {"season":"fall", "archived":false}
                }
                """);
    }

    /** Fails after staging so preservation and cleanup can be checked without file permissions. */
    private static class FailingReplacementStore extends FileAppDataAccessObject {
        private boolean failReplacement = true;

        FailingReplacementStore(Path file) {
            super(file);
        }

        @Override
        void replaceFile(Path temporary, Path target) throws IOException {
            if (failReplacement) {
                throw new IOException("The store could not be replaced");
            }
            super.replaceFile(temporary, target);
        }
    }
}
