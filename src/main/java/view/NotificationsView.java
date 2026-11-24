package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import interface_adapter.view_notifications.ViewNotificationsController;
import interface_adapter.view_notifications.ViewNotificationsViewModel;
import use_case.view_notifications.ViewNotificationsOutputData.NotificationData;
import use_case.view_notifications.ViewNotificationsOutputData.TeamData;

/** Displays the durable event change history for the reader's permitted teams. */
public class NotificationsView extends JPanel {
    private static final Color INK = new Color(25, 40, 65);
    private static final Color MUTED = new Color(90, 98, 110);
    private static final Color ERROR = new Color(180, 30, 30);
    private static final DateTimeFormatter NOTICE_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    private final ViewNotificationsController controller;
    private final ViewNotificationsViewModel viewModel;
    private final JComboBox<TeamOption> teams = new JComboBox<>();
    private final JTextArea feed = new JTextArea();
    private final JLabel feedback = new JLabel(" ");
    private boolean updatingTeams;

    /** Connects the page to the reading slice; call reset when the session changes. */
    public NotificationsView(ViewNotificationsController controller, ViewNotificationsViewModel viewModel) {
        this.controller = controller;
        this.viewModel = viewModel;
        setLayout(new BorderLayout(0, 18));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        add(buildHeader(), BorderLayout.NORTH);

        feed.setName("notificationFeed");
        feed.getAccessibleContext().setAccessibleName("Team event notifications");
        feed.setEditable(false);
        feed.setLineWrap(true);
        feed.setWrapStyleWord(true);
        feed.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        feed.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
        feed.setBackground(Color.WHITE);
        final JScrollPane scroll = new JScrollPane(feed);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        viewModel.addPropertyChangeListener(event -> render());
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refresh();
            }
        });
        render();
    }

    private JPanel buildHeader() {
        final JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        final JLabel title = new JLabel("Notifications");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(INK);
        header.add(title, BorderLayout.NORTH);

        final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        final JLabel teamLabel = new JLabel("Team");
        teamLabel.setLabelFor(teams);
        teams.setName("notificationTeam");
        teams.getAccessibleContext().setAccessibleName("Notification team filter");
        teams.setPreferredSize(new Dimension(260, 30));
        teams.addActionListener(event -> {
            if (!updatingTeams) {
                refresh();
            }
        });
        final JButton refresh = new JButton("Refresh");
        refresh.setName("refreshNotifications");
        refresh.addActionListener(event -> refresh());
        controls.add(teamLabel);
        controls.add(teams);
        controls.add(refresh);
        header.add(controls, BorderLayout.CENTER);
        feedback.setForeground(ERROR);
        feedback.putClientProperty("html.disable", true);
        header.add(feedback, BorderLayout.SOUTH);
        return header;
    }

    /** Reloads stored event changes without reconstructing or delivering messages. */
    public void refresh() {
        final TeamOption selected = (TeamOption) teams.getSelectedItem();
        controller.execute(selected == null ? null : selected.teamId);
    }

    /** Removes the previous account's feed and filter immediately. */
    public void reset() {
        viewModel.clear();
    }

    private void render() {
        updatingTeams = true;
        try {
            teams.removeAllItems();
            teams.addItem(new TeamOption("", "All my teams"));
            for (TeamData team : viewModel.getTeams()) {
                final TeamOption option = new TeamOption(team.getTeamId(), team.getName());
                teams.addItem(option);
                if (team.getTeamId().equals(viewModel.getSelectedTeamId())) {
                    teams.setSelectedItem(option);
                }
            }
        }
        finally {
            updatingTeams = false;
        }
        teams.setEnabled(!viewModel.getTeams().isEmpty());
        feedback.setText(viewModel.getErrorMessage().isEmpty() ? " " : viewModel.getErrorMessage());
        final StringBuilder text = new StringBuilder();
        for (NotificationData notification : viewModel.getNotifications()) {
            text.append(notification.getTeamName()).append("  ·  ")
                    .append(changeLabel(notification.getChangeType())).append("  ·  ")
                    .append(notification.getCreatedAt().format(NOTICE_TIME)).append('\n')
                    .append(notification.getMessage()).append("\n\n");
        }
        if (viewModel.getNotifications().isEmpty() && viewModel.getErrorMessage().isEmpty()) {
            text.append(emptyMessage());
        }
        feed.setText(text.toString());
        feed.setForeground(viewModel.getNotifications().isEmpty() ? MUTED : INK);
        feed.setCaretPosition(0);
        revalidate();
        repaint();
    }

    private String emptyMessage() {
        if (!viewModel.isLoaded()) {
            return "Refresh to see event changes for your teams.";
        }
        return viewModel.getTeams().isEmpty() ? "No teams are available for your account."
                : "No event changes for these teams yet.";
    }

    private static String changeLabel(String changeType) {
        return switch (changeType) {
            case "CREATED" -> "Event added";
            case "CHANGED" -> "Event changed";
            case "CANCELLED" -> "Event cancelled";
            default -> "Event update";
        };
    }

    /** Separates stored team identifiers from the labels in the filter. */
    private static final class TeamOption {
        private final String teamId;
        private final String name;

        private TeamOption(String teamId, String name) {
            this.teamId = teamId;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
