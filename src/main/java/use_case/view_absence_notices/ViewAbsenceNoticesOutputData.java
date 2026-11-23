package use_case.view_absence_notices;

import java.time.LocalDateTime;
import java.util.List;

/** Immutable form choices and authorized notice history. */
public record ViewAbsenceNoticesOutputData(List<ChildRow> children, String selectedPlayerUsername,
                                          List<EventRow> events, List<NoticeRow> notices,
                                          boolean canSubmit) {
    public ViewAbsenceNoticesOutputData {
        children = List.copyOf(children);
        events = List.copyOf(events);
        notices = List.copyOf(notices);
    }

    public record ChildRow(String username, String displayName) { }

    public record EventRow(String eventId, String teamName, String eventType,
                           LocalDateTime dateTime, String location) { }

    public record NoticeRow(String noticeId, String parentUsername, String parentName,
                            String playerUsername, String playerName, String eventId,
                            String teamName, String eventType, LocalDateTime dateTime,
                            String location, String reason, LocalDateTime submittedAt) { }
}
