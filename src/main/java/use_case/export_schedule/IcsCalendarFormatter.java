package use_case.export_schedule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import entity.Event;

/**
 * Turns events into iCalendar (.ics) text that Google Calendar, Outlook and
 * Apple Calendar can import. Moved here from the old view-layer exporter.
 *
 * Pure text building with no file access, so the interactor's tests can check
 * the exact output. Only this slice uses it.
 */
final class IcsCalendarFormatter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final String CRLF = "\r\n";

    private IcsCalendarFormatter() {
    }

    /**
     * Builds the calendar.
     *
     * @param calName calendar display name (e.g. the team name)
     * @param events  events to include, already in the order to write them; every
     *                one must have a date (the interactor leaves out the rest)
     * @return the whole .ics file as text
     */
    static String format(String calName, List<Event> events) {
        final StringBuilder out = new StringBuilder();
        out.append("BEGIN:VCALENDAR").append(CRLF);
        out.append("VERSION:2.0").append(CRLF);
        out.append("PRODID:-//Team Sports Management Platform//CSC207//EN").append(CRLF);
        out.append("CALSCALE:GREGORIAN").append(CRLF);
        out.append("X-WR-CALNAME:").append(escape(calName)).append(CRLF);

        final String stamp = LocalDateTime.now().format(DT);

        for (Event event : events) {
            final LocalDateTime start = event.getDateTime();
            // Events have no end time, so assume an hour like the calendar does.
            final LocalDateTime end = start.plusHours(1);

            out.append("BEGIN:VEVENT").append(CRLF);
            // A stable UID means importing again updates entries instead of
            // duplicating them.
            out.append("UID:").append(event.getEventId()).append("@teamsports").append(CRLF);
            out.append("DTSTAMP:").append(stamp).append(CRLF);
            out.append("DTSTART:").append(start.format(DT)).append(CRLF);
            out.append("DTEND:").append(end.format(DT)).append(CRLF);
            out.append("SUMMARY:").append(escape(summaryFor(event))).append(CRLF);
            if (event.getLocation() != null && !event.getLocation().isBlank()) {
                out.append("LOCATION:").append(escape(event.getLocation())).append(CRLF);
            }
            out.append("CATEGORIES:").append(event.getType().name()).append(CRLF);
            out.append("STATUS:").append(event.isCancelled() ? "CANCELLED" : "CONFIRMED")
                    .append(CRLF);
            out.append("END:VEVENT").append(CRLF);
        }
        out.append("END:VCALENDAR").append(CRLF);
        return out.toString();
    }

    /**
     * "Practice", "Game" or "Game vs Rivals". Cancelled events keep the prefix
     * too, since some calendar apps ignore STATUS.
     */
    private static String summaryFor(Event event) {
        String title;
        if (event.getType() == Event.EventType.GAME) {
            title = "Game";
            if (event.getOpponent() != null && !event.getOpponent().isBlank()) {
                title += " vs " + event.getOpponent();
            }
        }
        else {
            title = "Practice";
        }
        return event.isCancelled() ? "Cancelled: " + title : title;
    }

    /** Escape per RFC 5545: backslash, semicolon, comma, newline. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }
}
