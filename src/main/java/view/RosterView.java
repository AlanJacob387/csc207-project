package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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

/**
 * The Roster page. Everyone allowed on the page sees the team list; admins also
 * get forms to create teams, assign coaches, and add or remove players.
 */
public class RosterView extends JPanel {
    private static final Color SUCCESS = new Color(30, 120, 60);
    private static final Color ERROR = new Color(180, 40, 40);

    private final ViewRosterController viewController;
    private final Session session;

    private final JTextArea rosterArea = new JTextArea();
    private final JLabel status = new JLabel(" ");
    private final JPanel adminPanel = new JPanel();

    private final JTextField newTeamId = new JTextField(10);
    private final JTextField newTeamName = new JTextField(12);
    private final JTextField newTeamSport = new JTextField(10);
    private final JTextField coachTeamId = new JTextField(10);
    private final JTextField coachUsername = new JTextField(12);
    private final JTextField rosterTeamId = new JTextField(10);
    private final JTextField playerUsername = new JTextField(12);

    /** Connects the page to the four roster use cases and to session changes. */
    public RosterView(CreateTeamController createTeamController, CreateTeamViewModel createTeamViewModel,
                      AssignCoachController assignCoachController, AssignCoachViewModel assignCoachViewModel,
                      ManageRosterController manageRosterController,
                      ManageRosterViewModel manageRosterViewModel,
                      ViewRosterController viewController, ViewRosterViewModel viewModel,
                      Session session) {
        this.viewController = viewController;
        this.session = session;

        setLayout(new BorderLayout(0, 16));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        final JLabel title = new JLabel("Roster");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        final JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> viewController.execute());
        final JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(title, BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        rosterArea.setEditable(false);
        rosterArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        add(new JScrollPane(rosterArea), BorderLayout.CENTER);

        final JButton createButton = new JButton("Create team");
        createButton.addActionListener(event -> createTeamController.execute(
                newTeamId.getText(), newTeamName.getText(), newTeamSport.getText()));
        final JButton assignButton = new JButton("Assign coach");
        assignButton.addActionListener(event -> assignCoachController.execute(
                coachTeamId.getText(), coachUsername.getText()));
        final JButton addButton = new JButton("Add player");
        addButton.addActionListener(event -> manageRosterController.addPlayer(
                rosterTeamId.getText(), playerUsername.getText()));
        final JButton removeButton = new JButton("Remove player");
        removeButton.addActionListener(event -> manageRosterController.removePlayer(
                rosterTeamId.getText(), playerUsername.getText()));

        adminPanel.setLayout(new BoxLayout(adminPanel, BoxLayout.Y_AXIS));
        adminPanel.setOpaque(false);
        adminPanel.add(row(new JLabel("New team ID"), newTeamId, new JLabel("Name"), newTeamName,
                new JLabel("Sport"), newTeamSport, createButton));
        adminPanel.add(row(new JLabel("Team ID"), coachTeamId, new JLabel("Coach username"),
                coachUsername, assignButton));
        adminPanel.add(row(new JLabel("Team ID"), rosterTeamId, new JLabel("Player username"),
                playerUsername, addButton, removeButton));

        final JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(adminPanel, BorderLayout.CENTER);
        south.add(status, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        viewModel.addPropertyChangeListener(event -> {
            rosterArea.setText(viewModel.getRosterText());
            if (!viewModel.getErrorMessage().isEmpty()) {
                showStatus(viewModel.getErrorMessage(), false);
            }
        });
        createTeamViewModel.addPropertyChangeListener(event -> showResult(
                createTeamViewModel.getSuccessMessage(), createTeamViewModel.getErrorMessage()));
        assignCoachViewModel.addPropertyChangeListener(event -> showResult(
                assignCoachViewModel.getSuccessMessage(), assignCoachViewModel.getErrorMessage()));
        manageRosterViewModel.addPropertyChangeListener(event -> showResult(
                manageRosterViewModel.getSuccessMessage(), manageRosterViewModel.getErrorMessage()));
        session.addPropertyChangeListener(event -> resetForSession());
        resetForSession();
    }

    /** Shows admin forms only to admins and reloads the list for the new user. */
    private void resetForSession() {
        adminPanel.setVisible(session.getRole() == Role.ADMIN);
        status.setText(" ");
        rosterArea.setText("");
        if (session.isLoggedIn()) {
            viewController.execute();
        }
    }

    /** Shows the outcome of an admin action, and reloads the list after a saved change. */
    private void showResult(String successMessage, String errorMessage) {
        if (errorMessage.isEmpty()) {
            showStatus(successMessage, true);
            viewController.execute();
        }
        else {
            showStatus(errorMessage, false);
        }
    }

    private void showStatus(String message, boolean success) {
        status.setForeground(success ? SUCCESS : ERROR);
        status.setText(message);
    }

    private static JPanel row(java.awt.Component... components) {
        final JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        for (java.awt.Component component : components) {
            row.add(component);
        }
        return row;
    }
}
