package use_case.browse_events;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import entity.Event;
import use_case.current_user.CurrentUserInterface;

/**
 * Interactor for the Browse Events use case.
 *
 * Lists every event of the signed-in user's teams, narrowed by type and team.
 * Filtering happens here rather than in the view so the view never holds
 * events it isn't meant to show. A team filter is checked against the user's
 * own teams, so it can't be used to read another team's schedule.
 */
public class BrowseEventsInteractor implements BrowseEventsInputBoundary {

    private final BrowseEventsDataAccessInterface dataAccess;
    private final BrowseEventsOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    /**
     * Creates the interactor.
     *
     * @param dataAccess  where teams and events are read from
     * @param presenter   where the result is sent
     * @param currentUser the port that says who is signed in
     */
    public BrowseEventsInteractor(BrowseEventsDataAccessInterface dataAccess,
                                  BrowseEventsOutputBoundary presenter,
                                  CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(BrowseEventsInputData inputData) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in to see your events.");
            return;
        }

        final Event.EventType typeFilter;
        if (inputData.getType() == null) {
            typeFilter = null;
        }
        else {
            try {
                typeFilter = Event.EventType.valueOf(inputData.getType());
            }
            catch (IllegalArgumentException ex) {
                presenter.prepareFailView("Unknown event type: " + inputData.getType());
                return;
            }
        }
        final String teamFilter = inputData.getTeamId();
        final boolean filtered = typeFilter != null || teamFilter != null;

        final Map<String, String> teamNames = new LinkedHashMap<>();
        final List<Event> events;
        try {
            final List<String> teamIds = dataAccess.getTeamIdsForMember(username);
            if (teamIds.isEmpty()) {
                presenter.prepareSuccessView(new BrowseEventsOutputData(
                        new ArrayList<>(), new ArrayList<>(), false, filtered));
                return;
            }
            if (teamFilter != null && !teamIds.contains(teamFilter)) {
                presenter.prepareFailView("You're not on that team.");
                return;
            }
            for (String teamId : teamIds) {
                final String name = dataAccess.getTeamName(teamId);
                teamNames.put(teamId, name != null ? name : teamId);
            }
            events = dataAccess.getEventsForTeams(
                    teamFilter != null ? List.of(teamFilter) : teamIds);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not read your events: " + ex.getMessage());
            return;
        }

        final List<Event> matching = new ArrayList<>();
        for (Event event : events) {
            if (typeFilter == null || event.getType() == typeFilter) {
                matching.add(event);
            }
        }
        matching.sort(Comparator.comparing(Event::getDateTime,
                Comparator.nullsLast(Comparator.naturalOrder())));

        final List<BrowseEventsOutputData.EventRow> rows = new ArrayList<>();
        for (Event event : matching) {
            rows.add(new BrowseEventsOutputData.EventRow(
                    event.getEventId(),
                    event.getTeamId(),
                    teamNames.getOrDefault(event.getTeamId(), event.getTeamId()),
                    event.getType().name(),
                    event.getDateTime(),
                    event.getLocation(),
                    event.getOpponent(),
                    event.isCancelled()));
        }

        final List<BrowseEventsOutputData.TeamOption> teams = new ArrayList<>();
        for (Map.Entry<String, String> team : teamNames.entrySet()) {
            teams.add(new BrowseEventsOutputData.TeamOption(team.getKey(), team.getValue()));
        }
        presenter.prepareSuccessView(new BrowseEventsOutputData(rows, teams, true, filtered));
    }
}
