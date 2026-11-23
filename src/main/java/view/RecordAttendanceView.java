package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.HierarchyEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import entity.AttendanceRecord;
import interface_adapter.Session;
import interface_adapter.edit_attendance.EditAttendanceController;
import interface_adapter.edit_attendance.EditAttendanceViewModel;
import interface_adapter.list_available_events.ListAvailableEventsController;
import interface_adapter.list_available_events.ListAvailableEventsViewModel;
import interface_adapter.record_attendance.RecordAttendanceController;
import interface_adapter.record_attendance.RecordAttendanceViewModel;
import use_case.list_available_events.AvailableEvent;

/** A single coach-facing roster for both initial attendance and later corrections. */
public final class RecordAttendanceView extends JPanel {
    private static final Color ERROR_COLOR = new Color(160, 45, 45);
    private static final int NOTE_WIDTH = 29;

    private final RecordAttendanceController recordController;
    private final EditAttendanceController editController;
    private final ListAvailableEventsController listController;
    private final JComboBox<AvailableEvent> eventSelector = new JComboBox<>();
    private final JPanel rosterPanel = new JPanel(new GridBagLayout());
    private final Map<String, JComboBox<AttendanceRecord.Status>> selections = new LinkedHashMap<>();
    private final Map<String, JTextArea> notesFields = new LinkedHashMap<>();
    private final Map<String, AttendanceRecord.Status> savedStatuses = new LinkedHashMap<>();
    private final Map<String, String> savedNotes = new LinkedHashMap<>();
    private final JLabel feedback = new JLabel(" ");
    private final JLabel countLabel = new JLabel("Select an event to load its roster.");
    private boolean repopulatingEvents;

    public RecordAttendanceView(RecordAttendanceController recordController,
            RecordAttendanceViewModel recordModel, EditAttendanceController editController,
            EditAttendanceViewModel editModel, Session session,
            ListAvailableEventsController listController, ListAvailableEventsViewModel eventModel) {
        this.recordController = recordController;
        this.editController = editController;
        this.listController = listController;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        final JPanel form = L2FormStyles.page("Attendance",
                "Record or update player attendance and optional performance notes.");
        eventSelector.setPreferredSize(new Dimension(650, 34));
        L2FormStyles.field(form, "Team event", eventSelector);
        countLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(countLabel);
        form.add(Box.createVerticalStrut(10));

        rosterPanel.setBackground(Color.WHITE);
        final JScrollPane rosterScroll = new JScrollPane(rosterPanel);
        rosterScroll.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 235)));
        rosterScroll.getVerticalScrollBar().setUnitIncrement(18);
        rosterScroll.setPreferredSize(new Dimension(680, 340));
        rosterScroll.setMaximumSize(new Dimension(L2FormStyles.FORM_WIDTH, 420));
        rosterScroll.setAlignmentX(LEFT_ALIGNMENT);
        form.add(rosterScroll);
        form.add(Box.createVerticalStrut(12));

        final JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        footer.setBackground(Color.WHITE);
        footer.setAlignmentX(LEFT_ALIGNMENT);
        final JButton saveButton = new JButton("Save attendance");
        saveButton.addActionListener(e -> saveChanges());
        footer.add(saveButton);
        footer.add(feedback);
        form.add(footer);
        add(L2FormStyles.leftAligned(form), BorderLayout.CENTER);

        eventSelector.addActionListener(e -> {
            if (repopulatingEvents) { return; }
            clearEventState();
            final String eventId = currentEventId();
            if (eventId != null) { recordController.loadRoster(eventId); }
        });
        eventModel.addPropertyChangeListener(e -> {
            final String previousId = currentEventId();
            repopulatingEvents = true;
            eventSelector.removeAllItems();
            for (AvailableEvent item : eventModel.getEvents()) { eventSelector.addItem(item); }
            if (previousId != null) {
                for (int index = 0; index < eventSelector.getItemCount(); index++) {
                    if (eventSelector.getItemAt(index).getEventId().equals(previousId)) {
                        eventSelector.setSelectedIndex(index);
                        break;
                    }
                }
            }
            repopulatingEvents = false;
            clearEventState();
            feedback.setText(eventModel.getError());
            final String eventId = currentEventId();
            if (eventId != null) { recordController.loadRoster(eventId); }
        });
        recordModel.addPropertyChangeListener(e -> {
            if ("roster".equals(e.getPropertyName())) {
                renderRoster(recordModel.getRoster(), recordModel.getNotes());
            }
            else {
                feedback.setText(recordModel.getMessage());
                feedback.setForeground(recordModel.isSuccess() ? new Color(22, 101, 52) : ERROR_COLOR);
                if (recordModel.isSuccess()) { reloadSelectedRoster(); }
            }
        });
        editModel.addPropertyChangeListener(e -> {
            feedback.setText(editModel.getMessage());
            feedback.setForeground(editModel.isSuccess() ? new Color(22, 101, 52) : ERROR_COLOR);
            if (editModel.isSuccess()) { reloadSelectedRoster(); }
        });
        session.addPropertyChangeListener(e -> {
            repopulatingEvents = true;
            eventSelector.removeAllItems();
            repopulatingEvents = false;
            clearEventState();
            if (isShowing()) { this.listController.execute(); }
        });
        // Pages are reused by CardLayout; reload each time the coach opens Attendance.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                this.listController.execute();
            }
        });
    }

    private void reloadSelectedRoster() {
        final String eventId = currentEventId();
        if (eventId != null) { recordController.loadRoster(eventId); }
    }

    private void renderRoster(Map<String, AttendanceRecord.Status> roster,
            Map<String, String> notes) {
        selections.clear();
        notesFields.clear();
        savedStatuses.clear();
        savedNotes.clear();
        rosterPanel.removeAll();
        countLabel.setText(roster.size() + (roster.size() == 1 ? " player" : " players"));

        int rowIndex = 0;
        for (Map.Entry<String, AttendanceRecord.Status> entry : roster.entrySet()) {
            final String player = entry.getKey();
            final String savedNote = notes.getOrDefault(player, "");
            savedStatuses.put(player, entry.getValue());
            savedNotes.put(player, savedNote);

            final GridBagConstraints nameConstraints = new GridBagConstraints();
            nameConstraints.gridx = 0;
            nameConstraints.gridy = rowIndex;
            nameConstraints.anchor = GridBagConstraints.NORTHWEST;
            nameConstraints.insets = new Insets(12, 12, 12, 12);
            rosterPanel.add(new JLabel(player), nameConstraints);

            final JComboBox<AttendanceRecord.Status> status =
                    new JComboBox<>(AttendanceRecord.Status.values());
            if (entry.getValue() == null) { status.setSelectedIndex(-1); }
            else { status.setSelectedItem(entry.getValue()); }
            final GridBagConstraints statusConstraints = new GridBagConstraints();
            statusConstraints.gridx = 1;
            statusConstraints.gridy = rowIndex;
            statusConstraints.anchor = GridBagConstraints.NORTHWEST;
            statusConstraints.insets = new Insets(8, 4, 12, 12);
            rosterPanel.add(status, statusConstraints);
            selections.put(player, status);

            final JTextArea notesField = new JTextArea(2, NOTE_WIDTH);
            notesField.setLineWrap(true);
            notesField.setWrapStyleWord(true);
            notesField.setText(savedNote);
            final JScrollPane notesScroll = new JScrollPane(notesField);
            notesScroll.setPreferredSize(new Dimension(300, 52));
            final GridBagConstraints notesConstraints = new GridBagConstraints();
            notesConstraints.gridx = 2;
            notesConstraints.gridy = rowIndex;
            notesConstraints.weightx = 1;
            notesConstraints.fill = GridBagConstraints.HORIZONTAL;
            notesConstraints.anchor = GridBagConstraints.NORTHWEST;
            notesConstraints.insets = new Insets(6, 4, 12, 10);
            rosterPanel.add(notesScroll, notesConstraints);
            notesFields.put(player, notesField);
            rowIndex++;
        }
        if (roster.isEmpty()) {
            final GridBagConstraints empty = new GridBagConstraints();
            empty.gridx = 0;
            empty.gridy = 0;
            empty.insets = new Insets(16, 12, 16, 12);
            rosterPanel.add(new JLabel("No players on this roster."), empty);
        }
        rosterPanel.revalidate();
        rosterPanel.repaint();
    }

    private void saveChanges() {
        final String eventId = currentEventId();
        if (eventId == null || selections.isEmpty()) {
            showError("Select an event with a roster first.");
            return;
        }
        final Map<String, AttendanceRecord.Status> statuses = new LinkedHashMap<>();
        final Map<String, String> notes = new LinkedHashMap<>();
        String changedPlayer = null;
        int changes = 0;
        boolean hasUnrecordedPlayers = false;
        for (Map.Entry<String, JComboBox<AttendanceRecord.Status>> entry : selections.entrySet()) {
            final String player = entry.getKey();
            final AttendanceRecord.Status status =
                    (AttendanceRecord.Status) entry.getValue().getSelectedItem();
            if (status == null) {
                showError("Choose an attendance status for " + player + ".");
                return;
            }
            statuses.put(player, status);
            final String note = notesFields.get(player).getText().trim();
            notes.put(player, note);
            if (savedStatuses.get(player) == null) { hasUnrecordedPlayers = true; }
            if (!Objects.equals(status, savedStatuses.get(player))
                    || !Objects.equals(note, savedNotes.get(player))) {
                changedPlayer = player;
                changes++;
            }
        }
        if (changes == 0) {
            feedback.setText("No changes to save.");
            feedback.setForeground(new Color(100, 110, 123));
            return;
        }
        // A single correction of a saved record uses C2; initial or multi-player
        // updates use C1's atomic whole-roster persistence.
        if (changes == 1 && !hasUnrecordedPlayers) {
            editController.execute(eventId, changedPlayer,
                    statuses.get(changedPlayer), notes.get(changedPlayer));
        }
        else {
            recordController.execute(eventId, statuses, notes);
        }
    }

    private void showError(String message) {
        feedback.setForeground(ERROR_COLOR);
        feedback.setText(message);
    }

    private String currentEventId() {
        final AvailableEvent event = (AvailableEvent) eventSelector.getSelectedItem();
        return event == null ? null : event.getEventId();
    }

    private void clearEventState() {
        selections.clear();
        notesFields.clear();
        savedStatuses.clear();
        savedNotes.clear();
        rosterPanel.removeAll();
        rosterPanel.revalidate();
        rosterPanel.repaint();
        countLabel.setText("Select an event to load its roster.");
        feedback.setText(" ");
    }
}
