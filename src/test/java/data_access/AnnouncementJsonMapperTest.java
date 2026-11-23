package data_access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import entity.Announcement;

/** Checks the stored announcement format independently of file access. */
class AnnouncementJsonMapperTest {

    @Test
    void allAnnouncementFieldsRoundTripIncludingMessageFormatting() {
        final Announcement original = new Announcement("post-1", "team-1", "coach-1",
                "Bring a water bottle.\nPractice starts at 6 — rain or shine.",
                LocalDateTime.of(2026, 11, 9, 18, 30, 12, 123456789));

        final Announcement restored = EntityJsonMapper.announcementFromJson(
                new JSONObject(EntityJsonMapper.toJson(original).toString()));

        assertEquals(original.getAnnouncementId(), restored.getAnnouncementId());
        assertEquals(original.getTeamId(), restored.getTeamId());
        assertEquals(original.getAuthorUsername(), restored.getAuthorUsername());
        assertEquals(original.getMessage(), restored.getMessage());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    void aMissingRequiredFieldIsRejected() {
        for (String field : new String[]{"announcementId", "teamId", "authorUsername", "message", "createdAt"}) {
            final JSONObject json = EntityJsonMapper.toJson(sample());
            json.remove(field);

            assertThrows(JSONException.class, () -> EntityJsonMapper.announcementFromJson(json), field);
        }
    }

    @Test
    void anInvalidTimestampIsRejected() {
        final JSONObject json = EntityJsonMapper.toJson(sample());
        json.put("createdAt", "next Tuesday");

        assertThrows(DateTimeParseException.class, () -> EntityJsonMapper.announcementFromJson(json));
    }

    @Test
    void aNonStringMessageIsRejected() {
        final JSONObject json = EntityJsonMapper.toJson(sample());
        json.put("message", new JSONObject().put("text", "Practice starts at six"));

        assertThrows(JSONException.class, () -> EntityJsonMapper.announcementFromJson(json));
    }

    private Announcement sample() {
        return new Announcement("post-1", "team-1", "coach-1", "Practice starts at six",
                LocalDateTime.of(2026, 11, 9, 18, 0));
    }
}
