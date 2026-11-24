package use_case.view_notifications;

import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import entity.EventNotification;
import entity.Role;
import entity.Team;
import entity.User;
import use_case.current_user.CurrentUserInterface;

/** Applies the reader's current role and team membership to every event change. */
public class ViewNotificationsInteractor implements ViewNotificationsInputBoundary {
    private final ViewNotificationsDataAccessInterface dataAccess;
    private final ViewNotificationsOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    public ViewNotificationsInteractor(ViewNotificationsDataAccessInterface dataAccess,
                                       ViewNotificationsOutputBoundary presenter,
                                       CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(ViewNotificationsInputData inputData) {
        final String requestedTeam = inputData == null || inputData.getTeamId() == null
                ? "" : inputData.getTeamId().trim();
        final ViewNotificationsOutputData output;
        try {
            final String username = currentUser.getCurrentUsername();
            if (username == null || username.isBlank()) {
                presenter.prepareFailView("Sign in to view notifications.");
                return;
            }
            final User user = dataAccess.get(username);
            if (user == null || user.getRole() == null) {
                presenter.prepareFailView("Sign in to view notifications.");
                return;
            }
            final Set<String> children = user.getRole() == Role.PARENT
                    ? new HashSet<>(dataAccess.getLinkedPlayerUsernames(username)) : Set.of();
            final List<Team> accessible = dataAccess.getAllTeams().stream()
                    .filter(Objects::nonNull)
                    .filter(team -> canRead(user, team, children))
                    .sorted(Comparator.comparing(Team::getName).thenComparing(Team::getTeamId))
                    .toList();
            final Map<String, String> teamNames = new LinkedHashMap<>();
            for (Team team : accessible) {
                teamNames.put(team.getTeamId(), team.getName());
            }
            if (!requestedTeam.isEmpty() && !teamNames.containsKey(requestedTeam)) {
                presenter.prepareFailView("The selected team is unavailable or you cannot view its notifications.");
                return;
            }
            final List<ViewNotificationsOutputData.TeamData> teams = accessible.stream()
                    .map(team -> new ViewNotificationsOutputData.TeamData(team.getTeamId(), team.getName()))
                    .toList();
            final List<ViewNotificationsOutputData.NotificationData> notifications = teamNames.isEmpty()
                    ? List.of() : dataAccess.getEventNotifications().stream()
                            .filter(Objects::nonNull)
                            .filter(notification -> teamNames.containsKey(notification.getTeamId()))
                            .filter(notification -> requestedTeam.isEmpty()
                                    || requestedTeam.equals(notification.getTeamId()))
                            .sorted(Comparator.comparing(EventNotification::getCreatedAt).reversed()
                                    .thenComparing(EventNotification::getNotificationId))
                            .map(notification -> new ViewNotificationsOutputData.NotificationData(
                                    notification.getNotificationId(), notification.getEventId(),
                                    notification.getTeamId(), teamNames.get(notification.getTeamId()),
                                    notification.getChangeType(), notification.getMessage(), notification.getCreatedAt()))
                            .toList();
            if (!username.equals(currentUser.getCurrentUsername())) {
                presenter.prepareFailView("Your session changed. Refresh to view notifications.");
                return;
            }
            output = new ViewNotificationsOutputData(teams, requestedTeam, notifications);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not load notifications: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(output);
    }

    private static boolean canRead(User user, Team team, Set<String> children) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (user.getRole() == Role.COACH) {
            return user.getUsername().equals(team.getCoachUsername());
        }
        if (user.getRole() == Role.PLAYER) {
            return team.getPlayerUsernames().contains(user.getUsername());
        }
        return user.getRole() == Role.PARENT
                && team.getPlayerUsernames().stream().anyMatch(children::contains);
    }
}
