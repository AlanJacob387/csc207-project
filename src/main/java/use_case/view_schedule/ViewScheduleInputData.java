package use_case.view_schedule;

import java.time.LocalDate;

/**
 * Input data for the View Schedule use case: the dates the calendar is showing.
 * Both ends are inclusive, so a week is Sunday to Saturday.
 */
public class ViewScheduleInputData {
    private final LocalDate from;
    private final LocalDate to;

    /**
     * Creates the input data.
     *
     * @param from first day shown
     * @param to   last day shown
     */
    public ViewScheduleInputData(LocalDate from, LocalDate to) {
        this.from = from;
        this.to = to;
    }

    public LocalDate getFrom() {
        return from;
    }

    public LocalDate getTo() {
        return to;
    }
}
