package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.HierarchyEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import entity.Role;
import interface_adapter.Session;
import interface_adapter.link_parent_to_player.LinkParentToPlayerController;
import interface_adapter.link_parent_to_player.LinkParentToPlayerViewModel;
import use_case.link_parent_to_player.LinkParentToPlayerOutputData;

/** Allows an administrator to approve or remove parent/player relationships. */
public class ParentLinksView extends JPanel {
    private final LinkParentToPlayerController controller;
    private final LinkParentToPlayerViewModel viewModel;
    private final Session session;
    private final JComboBox<AccountOption> parents = new JComboBox<>();
    private final JComboBox<AccountOption> players = new JComboBox<>();
    private final DefaultListModel<LinkOption> links = new DefaultListModel<>();
    private final JList<LinkOption> linkList = new JList<>(links);
    private final JLabel feedback = new JLabel(" ");
    private final JButton link = new JButton("Link accounts");
    private final JButton unlink = new JButton("Unlink accounts");
    private boolean updating;

    public ParentLinksView(LinkParentToPlayerController controller,
                            LinkParentToPlayerViewModel viewModel, Session session) {
        this.controller = controller;
        this.viewModel = viewModel;
        this.session = session;
        setLayout(new BorderLayout(0, 16));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        final JPanel header = new JPanel(new BorderLayout(0, 16));
        header.setOpaque(false);
        final JLabel title = new JLabel("Parent links");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        header.add(title, BorderLayout.NORTH);
        final JPanel selectors = new JPanel(new GridLayout(2, 2, 12, 8));
        selectors.setOpaque(false);
        final JLabel parentLabel = new JLabel("Parent account");
        final JLabel playerLabel = new JLabel("Player account");
        parentLabel.setLabelFor(parents);
        playerLabel.setLabelFor(players);
        parents.setName("parentLinkParent");
        players.setName("parentLinkPlayer");
        selectors.add(parentLabel);
        selectors.add(playerLabel);
        selectors.add(parents);
        selectors.add(players);
        header.add(selectors, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        linkList.setName("parentLinkList");
        linkList.getAccessibleContext().setAccessibleName("Approved parent and player links");
        linkList.addListSelectionListener(event -> {
            if (!updating && !event.getValueIsAdjusting() && linkList.getSelectedValue() != null) {
                final var row = linkList.getSelectedValue().row();
                select(parents, row.parentUsername());
                select(players, row.playerUsername());
            }
        });
        add(new JScrollPane(linkList), BorderLayout.CENTER);
        final JPanel footer = new JPanel(new BorderLayout(0, 10));
        footer.setOpaque(false);
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        link.setName("parentLinkSave");
        unlink.setName("parentLinkDelete");
        link.addActionListener(event -> mutate("link"));
        unlink.addActionListener(event -> mutate("unlink"));
        final JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        buttons.add(refresh);
        buttons.add(link);
        buttons.add(unlink);
        footer.add(buttons, BorderLayout.SOUTH);
        feedback.putClientProperty("html.disable", true);
        footer.add(feedback, BorderLayout.NORTH);
        add(footer, BorderLayout.SOUTH);
        viewModel.addPropertyChangeListener(event -> render());
        session.addPropertyChangeListener(event -> {
            viewModel.clear();
            refresh();
        });
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refresh();
            }
        });
        viewModel.clear();
        refresh();
    }

    private void refresh() {
        if (session.isLoggedIn() && session.getCurrentRole() == Role.ADMIN) {
            controller.execute("load", null, null);
        }
    }

    private void mutate(String action) {
        final LinkOption selectedLink = linkList.getSelectedValue();
        if ("unlink".equals(action) && selectedLink != null) {
            controller.execute(action, selectedLink.row().parentUsername(), selectedLink.row().playerUsername());
            return;
        }
        final AccountOption parent = (AccountOption) parents.getSelectedItem();
        final AccountOption player = (AccountOption) players.getSelectedItem();
        controller.execute(action, parent == null ? null : parent.row().username(),
                player == null ? null : player.row().username());
    }

    private void render() {
        final var data = viewModel.getData();
        final AccountOption previousParent = (AccountOption) parents.getSelectedItem();
        final AccountOption previousPlayer = (AccountOption) players.getSelectedItem();
        updating = true;
        parents.removeAllItems();
        players.removeAllItems();
        links.clear();
        if (data != null) {
            data.parents().forEach(row -> parents.addItem(new AccountOption(row)));
            data.players().forEach(row -> players.addItem(new AccountOption(row)));
            data.links().forEach(row -> links.addElement(new LinkOption(row)));
            if (previousParent != null) {
                select(parents, previousParent.row().username());
            }
            if (previousPlayer != null) {
                select(players, previousPlayer.row().username());
            }
        }
        updating = false;
        final boolean canEdit = data != null && parents.getItemCount() > 0 && players.getItemCount() > 0;
        link.setEnabled(canEdit);
        unlink.setEnabled(data != null && !links.isEmpty());
        feedback.setText(data == null ? viewModel.getErrorMessage()
                : !data.message().isEmpty() ? data.message()
                : !canEdit ? "Create parent and player accounts before linking them."
                : links.isEmpty() ? "No parent/player links yet." : "Select a link to remove it, or choose accounts to link.");
    }

    private static void select(JComboBox<AccountOption> selector, String username) {
        for (int index = 0; index < selector.getItemCount(); index++) {
            if (selector.getItemAt(index).row().username().equals(username)) {
                selector.setSelectedIndex(index);
                return;
            }
        }
    }

    private record AccountOption(LinkParentToPlayerOutputData.AccountRow row) {
        @Override
        public String toString() {
            return row.displayName() + " (" + row.username() + ")";
        }
    }

    private record LinkOption(LinkParentToPlayerOutputData.LinkRow row) {
        @Override
        public String toString() {
            return row.parentName() + " (" + row.parentUsername() + ") → "
                    + row.playerName() + " (" + row.playerUsername() + ")";
        }
    }
}
