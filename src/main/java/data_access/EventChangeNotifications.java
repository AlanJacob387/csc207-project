package data_access;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import entity.Event;
import entity.EventNotification;

/**
 * Decides what the durable event-change history records when an event is saved,
 * so the local file and the hosted database write the same notification for the
 * same change.
 */
final class EventChangeNotifications {

    private EventChangeNotifications() {
    }

    /**
     * Describes what a save changed.
     *
     * @param previous the stored event, or null when the event is new
     * @param current  the event being saved
     * @param teamName the name to show for the event's team
     * @return the notification to record, or null when nothing worth announcing changed
     */
    static EventNotification between(Event previous, Event current, String teamName) {
        if (previous != null && previous.getTeamId().equals(current.getTeamId())
                && previous.getType() == current.getType()
                && Objects.equals(previous.getDateTime(), current.getDateTime())
                && Objects.equals(previous.getLocation(), current.getLocation())
                && Objects.equals(previous.getOpponent(), current.getOpponent())
                && previous.isCancelled() == current.isCancelled()) {
            return null;
        }
        final String change = previous == null ? "CREATED"
                : !previous.isCancelled() && current.isCancelled() ? "CANCELLED" : "CHANGED";
        final String verb = previous == null ? "scheduled"
                : "CANCELLED".equals(change) ? "cancelled" : "updated";
        final String message = teamName + " " + current.getType().name().toLowerCase(Locale.ROOT)
                + " " + verb + " for " + current.getDateTime() + " at " + current.getLocation() + ".";
        return new EventNotification(UUID.randomUUID().toString(), current.getEventId(),
                current.getTeamId(), change, message, LocalDateTime.now());
    }
}
