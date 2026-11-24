package use_case.link_parent_to_player;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import entity.ParentPlayerLink;
import entity.Role;
import entity.User;
import use_case.current_user.CurrentUserInterface;
import use_case.link_parent_to_player.LinkParentToPlayerOutputData.AccountRow;
import use_case.link_parent_to_player.LinkParentToPlayerOutputData.LinkRow;

/** Restricts parent-link administration to a currently stored administrator. */
public class LinkParentToPlayerInteractor implements LinkParentToPlayerInputBoundary {
    private final LinkParentToPlayerDataAccessInterface dataAccess;
    private final LinkParentToPlayerOutputBoundary presenter;
    private final CurrentUserInterface currentUser;

    public LinkParentToPlayerInteractor(LinkParentToPlayerDataAccessInterface dataAccess,
                                        LinkParentToPlayerOutputBoundary presenter,
                                        CurrentUserInterface currentUser) {
        this.dataAccess = dataAccess;
        this.presenter = presenter;
        this.currentUser = currentUser;
    }

    @Override
    public void execute(LinkParentToPlayerInputData input) {
        final String username = currentUser.getCurrentUsername();
        if (username == null || username.isBlank()) {
            presenter.prepareFailView("Sign in as an administrator to manage parent links.");
            return;
        }
        final String action = input == null ? "load" : input.action();
        if (!List.of("load", "link", "unlink").contains(action == null ? "" : action)) {
            presenter.prepareFailView("Choose whether to link or unlink the selected accounts.");
            return;
        }
        final LinkParentToPlayerOutputData output;
        try {
            final User administrator = dataAccess.get(username);
            if (administrator == null || administrator.getRole() != Role.ADMIN) {
                presenter.prepareFailView("Only an administrator can manage parent links.");
                return;
            }
            String message = "";
            if (!"load".equals(action)) {
                if (input.parentUsername() == null || input.playerUsername() == null) {
                    presenter.prepareFailView("Select a parent and a player.");
                    return;
                }
                if ("link".equals(action)) {
                    final User parent = dataAccess.get(input.parentUsername());
                    final User player = dataAccess.get(input.playerUsername());
                    if (parent == null || parent.getRole() != Role.PARENT
                            || player == null || player.getRole() != Role.PLAYER) {
                        presenter.prepareFailView("Select an existing parent and an existing player.");
                        return;
                    }
                    dataAccess.saveParentPlayerLink(new ParentPlayerLink(parent.getUsername(), player.getUsername()));
                    message = "Parent and player linked.";
                }
                else {
                    dataAccess.deleteParentPlayerLink(input.parentUsername(), input.playerUsername());
                    message = "Parent and player unlinked.";
                }
            }
            final Map<String, User> accounts = new LinkedHashMap<>();
            final List<AccountRow> parents = new ArrayList<>();
            final List<AccountRow> players = new ArrayList<>();
            for (User user : dataAccess.getAllUsers()) {
                accounts.put(user.getUsername(), user);
                final AccountRow row = new AccountRow(user.getUsername(), user.getDisplayName());
                if (user.getRole() == Role.PARENT) {
                    parents.add(row);
                }
                else if (user.getRole() == Role.PLAYER) {
                    players.add(row);
                }
            }
            final Comparator<AccountRow> order = Comparator.comparing(AccountRow::displayName)
                    .thenComparing(AccountRow::username);
            parents.sort(order);
            players.sort(order);
            final List<LinkRow> links = new ArrayList<>();
            for (ParentPlayerLink link : dataAccess.getParentPlayerLinks()) {
                final User parent = accounts.get(link.getParentUsername());
                final User player = accounts.get(link.getPlayerUsername());
                links.add(new LinkRow(link.getParentUsername(), parent == null ? link.getParentUsername()
                        : parent.getDisplayName(), link.getPlayerUsername(), player == null ? link.getPlayerUsername()
                        : player.getDisplayName()));
            }
            links.sort(Comparator.comparing(LinkRow::parentUsername).thenComparing(LinkRow::playerUsername));
            output = new LinkParentToPlayerOutputData(parents, players, links, message);
        }
        catch (IllegalStateException | UncheckedIOException ex) {
            presenter.prepareFailView("Could not manage parent links: " + ex.getMessage());
            return;
        }
        presenter.prepareSuccessView(output);
    }
}
