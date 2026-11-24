package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.HierarchyEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import interface_adapter.Session;
import interface_adapter.view_attendance_history.ViewAttendanceHistoryController;
import interface_adapter.view_attendance_history.ViewAttendanceHistoryViewModel;
import interface_adapter.view_stats.ViewStatsController;
import interface_adapter.view_stats.ViewStatsViewModel;
import use_case.view_attendance_history.AttendanceHistoryRow;
import use_case.view_stats.ViewStatsOutputData;

/** Player-only read-only attendance summary and history. */
public final class MyStatsView extends JPanel {
    public MyStatsView(ViewAttendanceHistoryController historyController,
            ViewAttendanceHistoryViewModel historyModel,
            ViewStatsController statsController, ViewStatsViewModel statsModel,
            Session session) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        final JPanel form = L2FormStyles.page("My Stats",
                "Review your attendance rate and past events.");
        final JLabel summary = new JLabel("No attendance records loaded yet.");
        L2FormStyles.field(form, "Attendance summary", summary);

        final DefaultTableModel model = new DefaultTableModel(
                new String[] {"Event", "Date", "Type", "Attendance", "Performance notes"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        final JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        final JScrollPane history = new JScrollPane(table);
        history.setPreferredSize(new Dimension(700, 280));
        history.setBorder(BorderFactory.createLineBorder(new Color(220, 225, 232)));
        L2FormStyles.field(form, "Attendance history", history);

        final JButton refresh = new JButton("Refresh history and stats");
        refresh.setAlignmentX(LEFT_ALIGNMENT);
        form.add(refresh);
        form.add(Box.createVerticalStrut(12));
        final JLabel error = new JLabel(" ");
        error.setForeground(new Color(160, 45, 45));
        error.setAlignmentX(LEFT_ALIGNMENT);
        form.add(error);
        add(L2FormStyles.leftAligned(form), BorderLayout.CENTER);

        final Runnable load = () -> {
            if (session.getRole() == entity.Role.PLAYER) {
                historyController.execute();
                statsController.execute();
            }
        };
        refresh.addActionListener(e -> load.run());
        historyModel.addPropertyChangeListener(e -> {
            model.setRowCount(0);
            for (AttendanceHistoryRow row : historyModel.getRows()) {
                model.addRow(new Object[] {row.getEventId(), row.getDate(), row.getType(),
                    row.getStatus(), row.getNotes() == null ? "" : row.getNotes()});
            }
            error.setText(historyModel.getError());
        });
        statsModel.addPropertyChangeListener(e -> {
            if (!statsModel.getError().isEmpty()) {
                error.setText(statsModel.getError());
                return;
            }
            final ViewStatsOutputData data = statsModel.getData();
            summary.setText(String.format(
                    "Present: %d    Absent: %d    Excused: %d    Attendance rate: %.1f%%",
                    data.getPresent(), data.getAbsent(), data.getExcused(),
                    data.getAttendanceRate() * 100));
        });
        session.addPropertyChangeListener(e -> {
            model.setRowCount(0);
            summary.setText("No attendance records loaded yet.");
            error.setText(" ");
            if (isShowing()) { load.run(); }
        });
        // Refresh when CardLayout brings this page to the foreground.
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                load.run();
            }
        });
    }
}
