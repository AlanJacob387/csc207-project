package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import entity.Role;
import interface_adapter.Session;
import interface_adapter.assign_coach.AssignCoachController;
import interface_adapter.assign_coach.AssignCoachViewModel;
import interface_adapter.create_team.CreateTeamController;
import interface_adapter.create_team.CreateTeamViewModel;
import interface_adapter.manage_roster.ManageRosterController;
import interface_adapter.manage_roster.ManageRosterViewModel;
import interface_adapter.view_roster.ViewRosterController;
import interface_adapter.view_roster.ViewRosterViewModel;
import use_case.view_roster.ViewRosterOutputData.Person;
import use_case.view_roster.ViewRosterOutputData.TeamRoster;

/**
 * The Roster page. Pick a team on the left to see its coach and players on the
 * right. Admins change the coach and roster with dropdowns and buttons, and
 * create teams with the "+ New team" button. Coaches see their teams read-only.
 */
public class RosterView extends JPanel {
    private static final Color INK = new Color(33, 37, 41);
    private static final Color MUTED = new Color(110, 117, 125);
    private static final Color SUCCESS = new Color(30, 120, 60);
    private static final Color ERROR = new Color(180, 40, 40);
    private static final String EMPTY_CARD = "empty";
    private static final String DETAIL_CARD = "detail";

    private final CreateTeamController createTeamController;
    private final AssignCoachController assignCoachController;
    private final ManageRosterController manageRosterController;
    private final ViewRosterController viewController;
    private final ViewRosterViewModel viewModel;
    private final Session session;

    private final JButton newTeamButton = new JButton("+ New team");
    private final DefaultListModel<Choice> teamListModel = new DefaultListModel<>();
    private final JList<Choice> teamList = new JList<>(teamListModel);
    private final CardLayout cards = new CardLayout();
    private final JPanel detailArea = new JPanel(cards);
    private final JLabel emptyMessage = new JLabel("", JLabel.CENTER);

    private final JLabel teamTitle = new JLabel();
    private final JLabel teamSubtitle = new JLabel();
    private final JLabel coachName = new JLabel();
    private final JComboBox<Choice> coachChoices = new JComboBox<>();
    private final JButton saveCoachButton = new JButton("Save coach");
    private final DefaultListModel<Choice> playerListModel = new DefaultListModel<>();
    private final JList<Choice> playerList = new JList<>(playerListModel);
    private final JButton removePlayerButton = new JButton("Remove selected player");
    private final JPanel addPlayerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JComboBox<Choice> addPlayerChoices = new JComboBox<>();
    private final JButton addPlayerButton = new JButton("Add");
    private final JLabel status = new JLabel(" ");

    private final JTextField newTeamId = new JTextField(16);
    private final JTextField newTeamName = new JTextField(16);
    private final JTextField newTeamSport = new JTextField(16);

    /** The team shown on the right, kept across reloads. Null when none is chosen. */
    private String selectedTeamId;
    /** True while the team list is being refilled, so selection events are ignored. */
    private boolean rebuilding;

    /** Connects the page to the four roster use cases and to session changes. */
    public RosterView(CreateTeamController createTeamController, CreateTeamViewModel createTeamViewModel,
                      AssignCoachController assignCoachController, AssignCoachViewModel assignCoachViewModel,
                      ManageRosterController manageRosterController,
                      ManageRosterViewModel manageRosterViewModel,
                      ViewRosterController viewController, ViewRosterViewModel viewModel,
                      Session session) {
        this.createTeamController = createTeamController;
        this.assignCoachController = assignCoachController;
        this.manageRosterController = manageRosterController;
        this.viewController = viewController;
        this.viewModel = viewModel;
        this.session = session;

        setLayout(new BorderLayout(0, 16));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        status.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        add(status, BorderLayout.SOUTH);

        viewModel.addPropertyChangeListener(event -> showTeams());
        createTeamViewModel.addPropertyChangeListener(event -> afterCreateTeam(
                createTeamViewModel.getSuccessMessage(), createTeamViewModel.getErrorMessage()));
        assignCoachViewModel.addPropertyChangeListener(event -> afterChange(
                assignCoachViewModel.getSuccessMessage(), assignCoachViewModel.getErrorMessage()));
        manageRosterViewModel.addPropertyChangeListener(event -> afterChange(
                manageRosterViewModel.getSuccessMessage(), manageRosterViewModel.getErrorMessage()));
        session.addPropertyChangeListener(event -> resetForSession());
        resetForSession();
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JPanel buildHeader() {
        final JLabel title = new JLabel("Roster");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        title.setForeground(INK);
        newTeamButton.addActionListener(event -> openNewTeamDialog());

        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(newTeamButton, BorderLayout.EAST);
        return header;
    }

    private JPanel buildBody() {
        teamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        teamList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        teamList.setFixedCellHeight(32);
        teamList.getAccessibleContext().setAccessibleName("Teams");
        teamList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !rebuilding) {
                final Choice choice = teamList.getSelectedValue();
                selectedTeamId = choice == null ? null : choice.id;
                showSelectedTeam();
            }
        });
        final JScrollPane teamScroll = new JScrollPane(teamList);
        teamScroll.setPreferredSize(new Dimension(240, 0));
        teamScroll.setBorder(BorderFactory.createTitledBorder("Teams"));

        emptyMessage.setForeground(MUTED);
        emptyMessage.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));
        detailArea.setOpaque(false);
        detailArea.add(emptyMessage, EMPTY_CARD);
        detailArea.add(buildTeamDetails(), DETAIL_CARD);

        final JPanel body = new JPanel(new BorderLayout(20, 0));
        body.setOpaque(false);
        body.add(teamScroll, BorderLayout.WEST);
        body.add(detailArea, BorderLayout.CENTER);
        return body;
    }

    private JPanel buildTeamDetails() {
        teamTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        teamTitle.setForeground(INK);
        teamSubtitle.setForeground(MUTED);

        coachChoices.setPreferredSize(new Dimension(260, 30));
        coachChoices.getAccessibleContext().setAccessibleName("Coach");
        saveCoachButton.addActionListener(event -> saveCoach());
        final JPanel coachRow = row(sectionLabel("Coach"), coachName, coachChoices, saveCoachButton);

        playerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playerList.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        playerList.setVisibleRowCount(8);
        playerList.getAccessibleContext().setAccessibleName("Players on this team");
        final JScrollPane playerScroll = new JScrollPane(playerList);
        playerScroll.setPreferredSize(new Dimension(380, 200));
        playerScroll.setMaximumSize(new Dimension(380, 260));
        playerScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        removePlayerButton.addActionListener(event -> removeSelectedPlayer());

        addPlayerChoices.setPreferredSize(new Dimension(260, 30));
        addPlayerChoices.getAccessibleContext().setAccessibleName("Player to add");
        addPlayerButton.addActionListener(event -> addChosenPlayer());
        addPlayerRow.setOpaque(false);
        addPlayerRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        addPlayerRow.add(sectionLabel("Add player"));
        addPlayerRow.add(addPlayerChoices);
        addPlayerRow.add(addPlayerButton);

        final JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
        details.add(leftAligned(teamTitle));
        details.add(leftAligned(teamSubtitle));
        details.add(spacer(20));
        details.add(coachRow);
        details.add(spacer(20));
        details.add(leftAligned(sectionLabel("Players")));
        details.add(spacer(6));
        details.add(playerScroll);
        details.add(spacer(6));
        details.add(leftAligned(removePlayerButton));
        details.add(spacer(20));
        details.add(addPlayerRow);

        final JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(details, BorderLayout.NORTH);
        return wrapper;
    }

    // ------------------------------------------------------------------
    // Showing data
    // ------------------------------------------------------------------

    /** Shows admin-only controls to admins and reloads the page for the new user. */
    private void resetForSession() {
        final boolean isAdmin = session.getRole() == Role.ADMIN;
        newTeamButton.setVisible(isAdmin);
        coachChoices.setVisible(isAdmin);
        saveCoachButton.setVisible(isAdmin);
        coachName.setVisible(!isAdmin);
        removePlayerButton.setVisible(isAdmin);
        addPlayerRow.setVisible(isAdmin);
        selectedTeamId = null;
        status.setText(" ");
        if (session.isLoggedIn()) {
            viewController.execute();
        }
    }

    /** Refills the team list after a reload, keeping the same team selected when it still exists. */
    private void showTeams() {
        rebuilding = true;
        teamListModel.clear();
        int selectedIndex = -1;
        for (TeamRoster team : viewModel.getTeams()) {
            if (team.getTeamId().equals(selectedTeamId)) {
                selectedIndex = teamListModel.size();
            }
            teamListModel.addElement(new Choice(team.getTeamId(), team.getName()));
        }
        if (selectedIndex == -1 && !teamListModel.isEmpty()) {
            selectedIndex = 0;
        }
        teamList.setSelectedIndex(selectedIndex);
        rebuilding = false;

        final Choice selected = teamList.getSelectedValue();
        selectedTeamId = selected == null ? null : selected.id;
        if (!viewModel.getErrorMessage().isEmpty()) {
            showStatus(viewModel.getErrorMessage(), false);
        }
        showSelectedTeam();
    }

    /** Fills the right-hand side with the chosen team, or explains why it is empty. */
    private void showSelectedTeam() {
        final TeamRoster team = findTeam(selectedTeamId);
        if (team == null) {
            if (!viewModel.getTeams().isEmpty()) {
                emptyMessage.setText("Select a team on the left.");
            }
            else if (session.getRole() == Role.ADMIN) {
                emptyMessage.setText("No teams yet. Click \"+ New team\" to create one.");
            }
            else {
                emptyMessage.setText("You are not the coach of any team yet.");
            }
            cards.show(detailArea, EMPTY_CARD);
            return;
        }

        teamTitle.setText(team.getName());
        teamSubtitle.setText(team.getSport() + "  ·  Team ID: " + team.getTeamId());
        coachName.setText(team.getCoach() == null ? "No coach yet" : label(team.getCoach()));

        final DefaultComboBoxModel<Choice> coachModel = new DefaultComboBoxModel<>();
        for (Person coach : viewModel.getCoaches()) {
            final Choice choice = new Choice(coach.getUsername(), label(coach));
            coachModel.addElement(choice);
            if (team.getCoach() != null && coach.getUsername().equals(team.getCoach().getUsername())) {
                coachModel.setSelectedItem(choice);
            }
        }
        if (team.getCoach() == null) {
            coachModel.setSelectedItem(null);
        }
        coachChoices.setModel(coachModel);

        playerListModel.clear();
        final List<String> onTeam = new ArrayList<>();
        for (Person player : team.getPlayers()) {
            playerListModel.addElement(new Choice(player.getUsername(), label(player)));
            onTeam.add(player.getUsername());
        }

        final DefaultComboBoxModel<Choice> addModel = new DefaultComboBoxModel<>();
        for (Person player : viewModel.getPlayers()) {
            if (!onTeam.contains(player.getUsername())) {
                addModel.addElement(new Choice(player.getUsername(), label(player)));
            }
        }
        addPlayerChoices.setModel(addModel);
        addPlayerChoices.setEnabled(addModel.getSize() > 0);
        addPlayerButton.setEnabled(addModel.getSize() > 0);

        cards.show(detailArea, DETAIL_CARD);
    }

    // ------------------------------------------------------------------
    // Admin actions
    // ------------------------------------------------------------------

    private void openNewTeamDialog() {
        final JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        form.add(new JLabel("Team name (for example: Raptors)"));
        form.add(newTeamName);
        form.add(new JLabel("Sport (for example: Basketball)"));
        form.add(newTeamSport);
        form.add(new JLabel("Team ID - short, no spaces (for example: u12-raptors)"));
        form.add(newTeamId);
        form.add(new JLabel("Coaches type the team ID on the New Event page."));

        final int answer = JOptionPane.showConfirmDialog(this, form, "New team",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer == JOptionPane.OK_OPTION) {
            createTeamController.execute(newTeamId.getText(), newTeamName.getText(),
                    newTeamSport.getText());
        }
    }

    private void saveCoach() {
        final Choice coach = (Choice) coachChoices.getSelectedItem();
        if (coach == null) {
            showStatus("Choose a coach first.", false);
            return;
        }
        assignCoachController.execute(selectedTeamId, coach.id);
    }

    private void removeSelectedPlayer() {
        final Choice player = playerList.getSelectedValue();
        if (player == null) {
            showStatus("Click a player in the list first.", false);
            return;
        }
        manageRosterController.removePlayer(selectedTeamId, player.id);
    }

    private void addChosenPlayer() {
        final Choice player = (Choice) addPlayerChoices.getSelectedItem();
        if (player == null) {
            showStatus("Choose a player to add first.", false);
            return;
        }
        manageRosterController.addPlayer(selectedTeamId, player.id);
    }

    /** After a new team is saved, clears the form and selects the new team. */
    private void afterCreateTeam(String successMessage, String errorMessage) {
        if (errorMessage.isEmpty()) {
            selectedTeamId = newTeamId.getText().trim();
            newTeamId.setText("");
            newTeamName.setText("");
            newTeamSport.setText("");
        }
        afterChange(successMessage, errorMessage);
    }

    /** Shows the outcome of an admin action, and reloads the page after a saved change. */
    private void afterChange(String successMessage, String errorMessage) {
        if (errorMessage.isEmpty()) {
            viewController.execute();
            showStatus(successMessage, true);
        }
        else {
            showStatus(errorMessage, false);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private TeamRoster findTeam(String teamId) {
        for (TeamRoster team : viewModel.getTeams()) {
            if (team.getTeamId().equals(teamId)) {
                return team;
            }
        }
        return null;
    }

    private void showStatus(String message, boolean success) {
        status.setForeground(success ? SUCCESS : ERROR);
        status.setText(message);
    }

    private static String label(Person person) {
        return person.getDisplayName() + " (" + person.getUsername() + ")";
    }

    private static JLabel sectionLabel(String text) {
        final JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        label.setForeground(INK);
        return label;
    }

    private static JPanel row(Component... components) {
        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Component component : components) {
            row.add(component);
        }
        return row;
    }

    private static Component leftAligned(JLabel label) {
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static Component leftAligned(JButton button) {
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    private static Component spacer(int height) {
        final JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        spacer.setAlignmentX(Component.LEFT_ALIGNMENT);
        spacer.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        spacer.setPreferredSize(new Dimension(0, height));
        return spacer;
    }

    /** A list or dropdown entry: the ID used by the use case and the text shown to the user. */
    private static final class Choice {
        private final String id;
        private final String text;

        private Choice(String id, String text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
