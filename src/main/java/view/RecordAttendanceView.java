package view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import entity.AttendanceRecord;
import interface_adapter.list_available_events.*;
import use_case.list_available_events.AvailableEvent;
import interface_adapter.Session;
import interface_adapter.record_attendance.RecordAttendanceController;
import interface_adapter.record_attendance.RecordAttendanceViewModel;

/** Coach's complete-roster attendance form. UI never decides authorization. */
public final class RecordAttendanceView extends JPanel {
    private final RecordAttendanceController controller;
    private final Session session;
    private final JPanel playersPanel = new JPanel(new GridLayout(0, 2, 12, 8));
    private final Map<String, JComboBox<AttendanceRecord.Status>> selections = new LinkedHashMap<>();
    private final JTextField eventIdField = new JTextField(15);
    private final JLabel feedback = new JLabel(" ");
    public RecordAttendanceView(RecordAttendanceController controller,
            RecordAttendanceViewModel viewModel,
            Session session, ListAvailableEventsController listController,
            ListAvailableEventsViewModel eventVm) {
        this.controller = controller;
        this.session = session;
        setLayout(new BorderLayout(12, 12));
        final JPanel top = new JPanel();
        top.add(new JLabel("Event ID:"));
        top.add(eventIdField);
        final JComboBox<AvailableEvent> events = new JComboBox<>();
        final JButton refreshEvents = new JButton("Show my events");
        refreshEvents.addActionListener(e -> listController.execute());
        eventVm.addPropertyChangeListener(e -> {
            events.removeAllItems();
            for (AvailableEvent item : eventVm.getEvents()) { events.addItem(item); }
        });
        events.addActionListener(e -> {
            final AvailableEvent selected = (AvailableEvent) events.getSelectedItem();
            if (selected != null) { eventIdField.setText(selected.getEventId()); }
        });
        top.add(refreshEvents);
        top.add(events);
        final JButton load = new JButton("Load roster");
        load.addActionListener(e -> loadRoster());
        top.add(load);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(playersPanel), BorderLayout.CENTER);
        final JPanel bottom = new JPanel();
        final JButton save = new JButton("Save attendance");
        save.addActionListener(e -> save());
        bottom.add(save);
        bottom.add(feedback);
        add(bottom, BorderLayout.SOUTH);
        viewModel.addPropertyChangeListener(e -> {
            feedback.setText(viewModel.getMessage());
            if ("roster".equals(e.getPropertyName())) {
                selections.clear();
                playersPanel.removeAll();
                for (Map.Entry<String, AttendanceRecord.Status> entry : viewModel.getRoster().entrySet()) {
                    playersPanel.add(new JLabel(entry.getKey()));
                    final JComboBox<AttendanceRecord.Status> combo =
                            new JComboBox<>(AttendanceRecord.Status.values());
                    combo.setSelectedItem(entry.getValue());
                    selections.put(entry.getKey(), combo);
                    playersPanel.add(combo);
                }
                playersPanel.revalidate();
                playersPanel.repaint();
            }
        });
        session.addPropertyChangeListener(e -> {
            selections.clear();
            playersPanel.removeAll();
            eventIdField.setText("");
            feedback.setText(" ");
            playersPanel.revalidate();
            playersPanel.repaint();
        });
    }
    private void loadRoster() {
        controller.loadRoster(eventIdField.getText().trim());
    }
    private void save() {
        final Map<String, AttendanceRecord.Status> statuses = new LinkedHashMap<>();
        for (Map.Entry<String, JComboBox<AttendanceRecord.Status>> entry : selections.entrySet()) {
            statuses.put(entry.getKey(), (AttendanceRecord.Status) entry.getValue().getSelectedItem());
        }
        controller.execute(eventIdField.getText().trim(), statuses);
    }
}
