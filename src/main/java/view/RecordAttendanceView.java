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
import entity.Event;
import entity.Team;
import interface_adapter.Session;
import interface_adapter.record_attendance.RecordAttendanceController;
import interface_adapter.record_attendance.RecordAttendanceViewModel;
import use_case.record_attendance.RecordAttendanceDataAccessInterface;

/** Coach's complete-roster attendance form. UI never decides authorization. */
public final class RecordAttendanceView extends JPanel {
    private final RecordAttendanceController controller;
    private final RecordAttendanceDataAccessInterface dataAccess;
    private final Session session;
    private final JPanel playersPanel = new JPanel(new GridLayout(0, 2, 12, 8));
    private final Map<String, JComboBox<AttendanceRecord.Status>> selections = new LinkedHashMap<>();
    private final JTextField eventIdField = new JTextField(15);
    private final JLabel feedback = new JLabel(" ");
    public RecordAttendanceView(RecordAttendanceController controller,
            RecordAttendanceViewModel viewModel, RecordAttendanceDataAccessInterface dataAccess,
            Session session) {
        this.controller = controller;
        this.dataAccess = dataAccess;
        this.session = session;
        setLayout(new BorderLayout(12, 12));
        final JPanel top = new JPanel();
        top.add(new JLabel("Event ID:"));
        top.add(eventIdField);
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
        selections.clear();
        playersPanel.removeAll();
        final Event event = dataAccess.getEvent(eventIdField.getText().trim());
        if (event == null) {
            feedback.setText("Event not found.");
        }
        else {
            final Team team = dataAccess.getTeam(event.getTeamId());
            if (team == null || !session.getCurrentUsername().equals(team.getCoachUsername())) {
                feedback.setText("Only the assigned coach may load this roster.");
            }
            else {
                final Map<String, AttendanceRecord.Status> existing = new LinkedHashMap<>();
                for (AttendanceRecord record : dataAccess.getAttendanceFor(event.getEventId())) {
                    existing.put(record.getPlayerUsername(), record.getStatus());
                }
                for (String player : team.getPlayerUsernames()) {
                    playersPanel.add(new JLabel(player));
                    final JComboBox<AttendanceRecord.Status> combo =
                            new JComboBox<>(AttendanceRecord.Status.values());
                    combo.setSelectedItem(existing.get(player));
                    selections.put(player, combo);
                    playersPanel.add(combo);
                }
                feedback.setText("Mark every player before saving.");
            }
        }
        playersPanel.revalidate();
        playersPanel.repaint();
    }
    private void save() {
        final Map<String, AttendanceRecord.Status> statuses = new LinkedHashMap<>();
        for (Map.Entry<String, JComboBox<AttendanceRecord.Status>> entry : selections.entrySet()) {
            statuses.put(entry.getKey(), (AttendanceRecord.Status) entry.getValue().getSelectedItem());
        }
        controller.execute(eventIdField.getText().trim(), statuses);
    }
}
