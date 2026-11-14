package use_case.export_schedule;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import entity.Event;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Export Schedule use case.
 *
 * Exports every event of the signed-in user's teams, past and future, as an
 * iCalendar file. Cancelled events are included and marked cancelled, so a
 * calendar that imported them earlier picks up the change. The file itself is
 * written by the ScheduleFileWriter gateway.
 */
public class ExportScheduleInteractor implements ExportScheduleInputBoundary {

    private static final String MULTI_TEAM_NAME = "Team Sports Schedule";

    private final ExportScheduleDataAccessInterface dataAccess;
    private final ScheduleFileWriter fileWriter;
    private final ExportScheduleOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where teams and events are read from
     * @param fileWriter  writes the finished calendar to disk
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public ExportScheduleInteractor(ExportScheduleDataAccessInterface dataAccess,
                                    ScheduleFileWriter fileWriter,
                                    ExportScheduleOutputBoundary presenter,
                                    CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.fileWriter = fileWriter;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(ExportScheduleInputData inputData) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in to export the schedule.");
            return;
        }
        final String targetPath = inputData.getTargetPath();
        if (targetPath == null || targetPath.isBlank()) {
            presenter.prepareFailView("Choose a file to export to.");
            return;
        }

        final List<String> teamIds;
        final String calName;
        final List<Event> events;
        try {
            teamIds = dataAccess.getTeamIdsForMember(username);
            if (teamIds.isEmpty()) {
                presenter.prepareFailView("You're not on any team yet.");
                return;
            }
            calName = calendarName(teamIds);
            events = new ArrayList<>(dataAccess.getEventsForTeams(teamIds));
        }
        catch (IllegalStateException ex) {
            presenter.prepareFailView("Could not read the schedule: " + ex.getMessage());
            return;
        }

        if (events.isEmpty()) {
            presenter.prepareNothingToExportView("There are no events to export yet.");
            return;
        }
        events.sort(Comparator.comparing(Event::getDateTime));

        final String content = IcsCalendarFormatter.format(calName, events);
        try {
            fileWriter.write(targetPath, content);
        }
        catch (ScheduleWriteException ex) {
            presenter.prepareFailView("Export failed. " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(new ExportScheduleOutputData(targetPath, events.size()));
    }

    /** One team's name, or a general title when the user is on several. */
    private String calendarName(List<String> teamIds) {
        if (teamIds.size() > 1) {
            return MULTI_TEAM_NAME;
        }
        final String name = dataAccess.getTeamName(teamIds.get(0));
        return name != null ? name : MULTI_TEAM_NAME;
    }
}
