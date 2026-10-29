package view;

import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import interface_adapter.create_event.CreateEventController;
import interface_adapter.create_event.CreateEventViewModel;

/**
 * Swing view for the Create Event use case (coach schedules a practice/game).
 */
public class CreateEventView extends JPanel implements PropertyChangeListener {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CreateEventController controller;
    private final CreateEventViewModel viewModel;

    private final JTextField teamIdField = new JTextField(20);
    private final JComboBox<String> typeBox = new JComboBox<>(new String[]{"PRACTICE", "GAME"});
    private final JTextField dateTimeField = new JTextField("2026-11-01 18:00", 20);
    private final JTextField locationField = new JTextField(20);
    private final JTextField opponentField = new JTextField(20);
    private final JLabel statusLabel = new JLabel(" ");

    public CreateEventView(CreateEventController controller, CreateEventViewModel viewModel) {
        this.controller = controller;
        this.viewModel = viewModel;
        this.viewModel.addPropertyChangeListener(this);

        setLayout(new GridLayout(0, 2, 8, 8));
        add(new JLabel("Team ID:"));
        add(teamIdField);
        add(new JLabel("Type:"));
        add(typeBox);
        add(new JLabel("Date/time (yyyy-MM-dd HH:mm):"));
        add(dateTimeField);
        add(new JLabel("Location:"));
        add(locationField);
        add(new JLabel("Opponent (games only):"));
        add(opponentField);

        final JButton createButton = new JButton("Create Event");
        createButton.addActionListener(e -> onCreate());
        add(createButton);
        add(statusLabel);
    }

    private void onCreate() {
        final LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(dateTimeField.getText().trim(), FORMAT);
        } catch (DateTimeParseException ex) {
            statusLabel.setText("Invalid date/time format.");
            return;
        }
        controller.execute(
                teamIdField.getText().trim(),
                (String) typeBox.getSelectedItem(),
                dateTime,
                locationField.getText().trim(),
                opponentField.getText().trim()
        );
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (!viewModel.getErrorMessage().isEmpty()) {
            statusLabel.setText(viewModel.getErrorMessage());
        } else {
            statusLabel.setText(viewModel.getSuccessMessage());
        }
    }
}