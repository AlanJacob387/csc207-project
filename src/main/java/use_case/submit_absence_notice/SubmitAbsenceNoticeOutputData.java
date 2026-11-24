package use_case.submit_absence_notice;

/** Identifies the saved notice so the form can refresh its history. */
public record SubmitAbsenceNoticeOutputData(String noticeId, String message) {
}
