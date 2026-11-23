package use_case.view_announcements;

import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import entity.Announcement;
import entity.Role;
import entity.Team;
import entity.User;

/** Lists posts only after the reader's membership in the selected team is checked. */
public class ViewAnnouncementsInteractor implements ViewAnnouncementsInputBoundary {
    private final ViewAnnouncementsDataAccessInterface dataAccess;
    private final ViewAnnouncementsOutputBoundary presenter;

    public ViewAnnouncementsInteractor(ViewAnnouncementsDataAccessInterface dataAccess,
                                      ViewAnnouncementsOutputBoundary presenter) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
    }

    @Override
    public void execute(ViewAnnouncementsInputData inputData) {
        if (inputData == null || inputData.getUsername() == null
                || inputData.getUsername().isBlank()) {
            presenter.prepareFailView("Sign in to view announcements.");
            return;
        }

        final String requestedTeamId = inputData.getTeamId() == null
                ? "" : inputData.getTeamId().trim();
        final ViewAnnouncementsOutputData outputData;
        try {
            final User user = dataAccess.get(inputData.getUsername());
            if (user == null) {
                presenter.prepareFailView("Sign in to view announcements.");
                return;
            }

            // Parent access remains closed until an authorized parent/player link is available.
            final List<Team> teams = user.getRole() == Role.PARENT ? List.of()
                    : dataAccess.getAllTeams().stream()
                            .filter(Objects::nonNull)
                            .filter(team -> canRead(user, team))
                            .sorted(Comparator.comparing(Team::getName).thenComparing(Team::getTeamId))
                            .toList();
            final List<ViewAnnouncementsOutputData.TeamData> teamData = teams.stream()
                    .map(team -> new ViewAnnouncementsOutputData.TeamData(team.getTeamId(), team.getName()))
                    .toList();
            final Team selectedTeam;
            if (requestedTeamId.isEmpty()) {
                selectedTeam = teams.isEmpty() ? null : teams.get(0);
            }
            else {
                selectedTeam = teams.stream()
                        .filter(team -> requestedTeamId.equals(team.getTeamId()))
                        .findFirst().orElse(null);
                if (selectedTeam == null) {
                    presenter.prepareFailView("The selected team is unavailable or you cannot view its announcements.");
                    return;
                }
            }

            if (selectedTeam == null) {
                outputData = new ViewAnnouncementsOutputData(teamData, null, List.of(), false);
            }
            else {
                final String teamId = selectedTeam.getTeamId();
                final List<ViewAnnouncementsOutputData.AnnouncementData> announcements =
                        dataAccess.getAnnouncementsForTeam(teamId).stream()
                                .filter(Objects::nonNull)
                                .filter(announcement -> teamId.equals(announcement.getTeamId()))
                                .sorted(Comparator.comparing(Announcement::getCreatedAt).reversed()
                                        .thenComparing(Announcement::getAnnouncementId))
                                .map(announcement -> new ViewAnnouncementsOutputData.AnnouncementData(
                                        announcement.getAnnouncementId(), announcement.getAuthorUsername(),
                                        announcement.getMessage(), announcement.getCreatedAt()))
                                .toList();
                outputData = new ViewAnnouncementsOutputData(teamData, teamId, announcements,
                        user.getRole() == Role.COACH);
            }
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not load announcements: " + ex.getMessage());
            return;
        }

        presenter.prepareSuccessView(outputData);
    }

    /** Applies role and current roster membership before any post query is made. */
    private static boolean canRead(User user, Team team) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() == Role.COACH) {
            return user.getUsername().equals(team.getCoachUsername());
        }
        return user.getRole() == Role.PLAYER && team.getPlayerUsernames().contains(user.getUsername());
    }
}
