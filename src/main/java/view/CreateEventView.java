package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import interface_adapter.create_event.CreateEventController;
import interface_adapter.create_event.CreateEventViewModel;

/** Coach's create-event form; business validation stays in the use case. */
public class CreateEventView extends JPanel implements PropertyChangeListener {
    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CreateEventController controller;
    private final CreateEventViewModel viewModel;
    private final JTextField teamIdField = new JTextField(25);
    private final JComboBox<String> typeBox =
            new JComboBox<>(new String[] {"PRACTICE", "GAME"});
    private final JTextField dateTimeField = new JTextField(25);
    private final JTextField locationField = new JTextField(25);
    private final JTextField opponentField = new JTextField(25);
    private final JLabel statusLabel = new JLabel(" ");

    public CreateEventView(CreateEventController controller, CreateEventViewModel viewModel) {
        this.controller = controller;
        this.viewModel = viewModel;
        viewModel.addPropertyChangeListener(this);
        setLayout(new BorderLayout());
        setBackground(java.awt.Color.WHITE);

        final JPanel form = L2FormStyles.page("New Event",
                "Schedule a practice or game for your team.");
        teamIdField.setPreferredSize(new Dimension(640, 34));
        L2FormStyles.field(form, "Team ID", teamIdField);
        L2FormStyles.field(form, "Event type", typeBox);
        dateTimeField.setText(LocalDateTime.now().plusDays(7).format(FORMAT));
        L2FormStyles.field(form, "Date and time (yyyy-MM-dd HH:mm)", dateTimeField);
        L2FormStyles.field(form, "Location", locationField);
        L2FormStyles.field(form, "Opponent (games only)", opponentField);
        opponentField.setEnabled(false);
        typeBox.addActionListener(e -> {
            final boolean game = "GAME".equals(typeBox.getSelectedItem());
            opponentField.setEnabled(game);
            if (!game) { opponentField.setText(""); }
        });

        final JButton createButton = new JButton("Create event");
        createButton.setAlignmentX(LEFT_ALIGNMENT);
        createButton.addActionListener(e -> onCreate());
        form.add(createButton);
        form.add(Box.createVerticalStrut(12));
        statusLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(statusLabel);
        add(L2FormStyles.leftAligned(form), BorderLayout.CENTER);
    }

    private void onCreate() {
        final LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(dateTimeField.getText().trim(), FORMAT);
        }
        catch (DateTimeParseException ex) {
            statusLabel.setText("Invalid date/time format. Use yyyy-MM-dd HH:mm.");
            return;
        }
        controller.execute(teamIdField.getText().trim(),
                (String) typeBox.getSelectedItem(), dateTime,
                locationField.getText().trim(), opponentField.getText().trim());
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        statusLabel.setText(!viewModel.getErrorMessage().isEmpty()
                ? viewModel.getErrorMessage() : viewModel.getSuccessMessage());
    }
}
