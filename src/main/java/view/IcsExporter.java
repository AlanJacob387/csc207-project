package view;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import view.CalendarView.CalendarEvent;

/**
 * Writes the calendar's events to an iCalendar (.ics) file that can be
 * imported into Google Calendar, Outlook, Apple Calendar, etc.
 *
 * Timed events get DTSTART/DTEND with times; untimed events are written
 * as all-day events (VALUE=DATE).
 *
 * TODO (Clean Architecture): promote this into a proper Export Schedule
 * use case — ExportScheduleInteractor in use_case/, with this file writer
 * behind a gateway interface in data_access/. For now it lives in the view
 * package so the UI template stays self-contained.
 */
public final class IcsExporter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String CRLF = "\r\n";

    private IcsExporter() {
    }

    /**
     * Writes all events to the given path as an .ics file.
     *
     * @param events   events keyed by date (the calendar's map)
     * @param calName  calendar display name (e.g. the team name)
     * @param target   file to write (should end in .ics)
     * @throws IOException if the file cannot be written
     */
    public static void export(Map<LocalDate, List<CalendarEvent>> events,
                              String calName, Path target) throws IOException {
        try (Writer w = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
            w.write("BEGIN:VCALENDAR" + CRLF);
            w.write("VERSION:2.0" + CRLF);
            w.write("PRODID:-//Team Sports Management Platform//CSC207//EN" + CRLF);
            w.write("CALSCALE:GREGORIAN" + CRLF);
            w.write("X-WR-CALNAME:" + escape(calName) + CRLF);

            final String stamp = LocalDateTime.now().format(DT);

            // TreeMap so events are written in date order
            for (Map.Entry<LocalDate, List<CalendarEvent>> entry
                    : new TreeMap<>(events).entrySet()) {
                final LocalDate date = entry.getKey();
                for (CalendarEvent ev : entry.getValue()) {
                    w.write("BEGIN:VEVENT" + CRLF);
                    w.write("UID:" + UUID.randomUUID() + "@teamsports" + CRLF);
                    w.write("DTSTAMP:" + stamp + CRLF);

                    if (ev.start == null) {
                        // All-day event: DATE value, DTEND is the next day (exclusive)
                        w.write("DTSTART;VALUE=DATE:" + date.format(D) + CRLF);
                        w.write("DTEND;VALUE=DATE:" + date.plusDays(1).format(D) + CRLF);
                    } else {
                        final LocalTime end = ev.end != null ? ev.end : ev.start.plusHours(1);
                        w.write("DTSTART:" + LocalDateTime.of(date, ev.start).format(DT) + CRLF);
                        w.write("DTEND:" + LocalDateTime.of(date, end).format(DT) + CRLF);
                    }

                    w.write("SUMMARY:" + escape(ev.title) + CRLF);
                    w.write("CATEGORIES:" + (ev.isGame ? "GAME" : "PRACTICE") + CRLF);
                    w.write("END:VEVENT" + CRLF);
                }
            }
            w.write("END:VCALENDAR" + CRLF);
        }
    }

    /** Escape per RFC 5545: backslash, semicolon, comma, newline. */
    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }
}