package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import entity.Role;
import interface_adapter.Session;
import interface_adapter.post_announcement.PostAnnouncementController;
import interface_adapter.post_announcement.PostAnnouncementViewModel;
import interface_adapter.view_announcements.ViewAnnouncementsController;
import interface_adapter.view_announcements.ViewAnnouncementsViewModel;
import use_case.view_announcements.ViewAnnouncementsOutputData.AnnouncementData;
import use_case.view_announcements.ViewAnnouncementsOutputData.TeamData;

/** Shows team posts and a composer for the selected team's authorized coach. */
public class AnnouncementsView extends JPanel {
    private static final Color INK = new Color(25, 40, 65);
    private static final Color MUTED = new Color(90, 98, 110);
    private static final Color ERROR = new Color(180, 30, 30);
    private static final DateTimeFormatter POST_TIME = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    private final PostAnnouncementController postController;
    private final PostAnnouncementViewModel postViewModel;
    private final ViewAnnouncementsController viewController;
    private final ViewAnnouncementsViewModel viewModel;
    private final Session session;
    private final JComboBox<TeamOption> teams = new JComboBox<>();
    private final JButton refreshButton = new JButton("Refresh");
    private final JTextArea posts = new JTextArea();
    private final JPanel composer = new JPanel(new BorderLayout(0, 8));
    private final JTextArea message = new JTextArea(4, 40);
    private final JButton postButton = new JButton("Post announcement");
    private final JLabel readFeedback = new JLabel(" ");
    private final JLabel postFeedback = new JLabel(" ");
    private boolean updatingTeams;

    /** Connects the page to the posting and reading slices and session changes. */
    public AnnouncementsView(PostAnnouncementController postController,
                             PostAnnouncementViewModel postViewModel,
                             ViewAnnouncementsController viewController,
                             ViewAnnouncementsViewModel viewModel,
                             Session session) {
        this.postController = postController;
        this.postViewModel = postViewModel;
        this.viewController = viewController;
        this.viewModel = viewModel;
        this.session = session;
        setLayout(new BorderLayout(0, 18));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        add(buildHeader(), BorderLayout.NORTH);

        posts.setName("announcementFeed");
        posts.getAccessibleContext().setAccessibleName("Team announcements");
        posts.setEditable(false);
        posts.setLineWrap(true);
        posts.setWrapStyleWord(true);
        posts.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        posts.setBorder(BorderFactory.createEmptyBorder(16, 0, 16, 0));
        posts.setBackground(Color.WHITE);
        final JScrollPane scroll = new JScrollPane(posts);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
        add(buildComposer(), BorderLayout.SOUTH);

        viewModel.addPropertyChangeListener(event -> renderAnnouncements());
        postViewModel.addPropertyChangeListener(event -> renderPostResult());
        session.addPropertyChangeListener(event -> resetForSession());
        resetForSession();
    }

    private JPanel buildHeader() {
        final JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);
        final JLabel title = new JLabel("Announcements");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(INK);
        header.add(title, BorderLayout.NORTH);

        final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        final JLabel teamLabel = new JLabel("Team");
        teamLabel.setLabelFor(teams);
        teams.setName("announcementTeam");
        teams.getAccessibleContext().setAccessibleName("Announcement team");
        teams.setPreferredSize(new Dimension(260, 30));
        teams.addActionListener(event -> selectTeam());
        refreshButton.addActionListener(event -> viewController.execute(selectedTeamId()));
        controls.add(teamLabel);
        controls.add(teams);
        controls.add(refreshButton);
        header.add(controls, BorderLayout.CENTER);
        readFeedback.setForeground(ERROR);
        readFeedback.putClientProperty("html.disable", true);
        header.add(readFeedback, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildComposer() {
        composer.setOpaque(false);
        final JLabel label = new JLabel("Post an announcement");
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        label.setLabelFor(message);
        composer.add(label, BorderLayout.NORTH);
        message.setName("announcementMessage");
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        message.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        composer.add(new JScrollPane(message), BorderLayout.CENTER);
        final JPanel actions = new JPanel(new BorderLayout(12, 0));
        actions.setOpaque(false);
        postButton.addActionListener(event -> postController.execute(selectedTeamId(), message.getText()));
        postFeedback.putClientProperty("html.disable", true);
        actions.add(postFeedback, BorderLayout.CENTER);
        actions.add(postButton, BorderLayout.EAST);
        composer.add(actions, BorderLayout.SOUTH);
        return composer;
    }

    private void selectTeam() {
        if (!updatingTeams && teams.getSelectedItem() != null) {
            message.setText("");
            postViewModel.clear();
            viewController.execute(selectedTeamId());
        }
    }

    private String selectedTeamId() {
        final TeamOption selected = (TeamOption) teams.getSelectedItem();
        return selected == null ? null : selected.teamId;
    }

    private void resetForSession() {
        message.setText("");
        postViewModel.clear();
        viewModel.clear();
        if (session.isLoggedIn()) {
            viewController.execute(null);
        }
    }

    private void renderAnnouncements() {
        updatingTeams = true;
        try {
            teams.removeAllItems();
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
        refreshButton.setEnabled(session.isLoggedIn());
        final boolean canPost = session.isLoggedIn() && viewModel.canPost();
        composer.setVisible(canPost);
        postButton.setEnabled(canPost);
        readFeedback.setText(viewModel.getErrorMessage().isEmpty() ? " " : viewModel.getErrorMessage());

        final StringBuilder feed = new StringBuilder();
        if (viewModel.getErrorMessage().isEmpty() && viewModel.getAnnouncements().isEmpty()) {
            feed.append(emptyMessage());
            posts.setForeground(MUTED);
        }
        else {
            posts.setForeground(INK);
            for (AnnouncementData announcement : viewModel.getAnnouncements()) {
                feed.append(announcement.getAuthorUsername()).append("  ·  ")
                        .append(announcement.getCreatedAt().format(POST_TIME)).append('\n')
                        .append(announcement.getMessage()).append("\n\n");
            }
        }
        posts.setText(feed.toString());
        posts.setCaretPosition(0);
        revalidate();
        repaint();
    }

    private String emptyMessage() {
        if (!session.isLoggedIn()) {
            return "Sign in to view announcements.";
        }
        if (viewModel.getTeams().isEmpty()) {
            return session.getCurrentRole() == Role.PARENT
                    ? "No linked teams are available for your account."
                    : "No teams are available for your account.";
        }
        return "No announcements for this team yet.";
    }

    private void renderPostResult() {
        if (!postViewModel.getSuccessMessage().isEmpty()) {
            message.setText("");
            viewController.execute(postViewModel.getPostedTeamId());
            postFeedback.setForeground(new Color(22, 101, 52));
            postFeedback.setText(postViewModel.getSuccessMessage());
        }
        else {
            postFeedback.setForeground(ERROR);
            postFeedback.setText(postViewModel.getErrorMessage().isEmpty()
                    ? " " : postViewModel.getErrorMessage());
        }
    }

    /** Keeps the stored team identifier separate from its display name. */
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
