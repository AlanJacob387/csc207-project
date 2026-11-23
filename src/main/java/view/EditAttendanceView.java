package view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import entity.AttendanceRecord;
import interface_adapter.Session;
import interface_adapter.edit_attendance.EditAttendanceController;
import interface_adapter.edit_attendance.EditAttendanceViewModel;
import interface_adapter.list_available_events.ListAvailableEventsController;
import interface_adapter.list_available_events.ListAvailableEventsViewModel;
import use_case.list_available_events.AvailableEvent;

/** Coach's correction form for previously recorded attendance. */
public final class EditAttendanceView extends JPanel {

    private static final int FIELD_WIDTH = 440;

    public EditAttendanceView(
            EditAttendanceController controller,
            EditAttendanceViewModel vm,
            Session session,
            ListAvailableEventsController listController,
            ListAvailableEventsViewModel eventVm) {

        setLayout(new BorderLayout());

        final JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        final JTextField event = new JTextField(30);
        final JTextField player = new JTextField(30);
        final JComboBox<AttendanceRecord.Status> status =
                new JComboBox<>(AttendanceRecord.Status.values());

        final JTextArea notes = new JTextArea(3, 30);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);

        final JScrollPane notesScroll = new JScrollPane(notes);
        notesScroll.setPreferredSize(new Dimension(FIELD_WIDTH, 85));

        final JComboBox<AvailableEvent> events = new JComboBox<>();
        events.setPreferredSize(new Dimension(320, 32));

        final JButton refresh = new JButton("My events");
        final JPanel eventSelector =
                new JPanel(new BorderLayout(8, 0));
        eventSelector.add(events, BorderLayout.CENTER);
        eventSelector.add(refresh, BorderLayout.EAST);

        final JButton save =
                new JButton("Save attendance changes");
        final JLabel feedback = new JLabel(" ");

        int row = 0;

        addRow(form, row++, "Event ID", event);
        addRow(form, row++, "Select event", eventSelector);
        addRow(form, row++, "Player username", player);
        addRow(form, row++, "Attendance status", status);
        addRow(form, row++, "Performance notes", notesScroll);

        final JPanel buttonPanel =
                new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.add(save);
        addRow(form, row++, "", buttonPanel);

        addRow(form, row, "", feedback);

        final JPanel wrapper =
                new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 16));
        wrapper.add(form);
        add(wrapper, BorderLayout.NORTH);

        refresh.addActionListener(e -> listController.execute());

        eventVm.addPropertyChangeListener(e -> {
            events.removeAllItems();
            for (AvailableEvent item : eventVm.getEvents()) {
                events.addItem(item);
            }
        });

        events.addActionListener(e -> {
            final AvailableEvent selected =
                    (AvailableEvent) events.getSelectedItem();
            if (selected != null) {
                event.setText(selected.getEventId());
            }
        });

        save.addActionListener(e -> controller.execute(
                event.getText().trim(),
                player.getText().trim(),
                (AttendanceRecord.Status) status.getSelectedItem(),
                notes.getText().trim()));

        vm.addPropertyChangeListener(
                e -> feedback.setText(vm.getMessage()));

        session.addPropertyChangeListener(e -> {
            event.setText("");
            player.setText("");
            notes.setText("");
            events.removeAllItems();
            status.setSelectedIndex(0);
            feedback.setText(" ");
        });
    }

    /** Adds a labelled control using consistent spacing. */
    private static void addRow(
            JPanel form, int row, String label, java.awt.Component field) {

        final GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0;
        left.gridy = row;
        left.anchor = GridBagConstraints.NORTHWEST;
        left.insets = new Insets(8, 0, 12, 20);

        form.add(new JLabel(label), left);

        final GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1;
        right.gridy = row;
        right.fill = GridBagConstraints.HORIZONTAL;
        right.weightx = 1.0;
        right.insets = new Insets(4, 0, 12, 0);

        form.add(field, right);
    }
}
