package use_case.submit_absence_notice;

/** The child, event, and reason selected by the signed-in parent. */
public record SubmitAbsenceNoticeInputData(String playerUsername, String eventId, String reason) {
}
