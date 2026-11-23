package view;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import interface_adapter.Session;
import interface_adapter.view_attendance_history.*;
import interface_adapter.view_stats.*;
import use_case.view_attendance_history.AttendanceHistoryRow;
import use_case.view_stats.ViewStatsOutputData;
/** Read-only history and summary page for the current player. */
public final class MyStatsView extends JPanel {
 public MyStatsView(ViewAttendanceHistoryController historyController,
         ViewAttendanceHistoryViewModel historyModel,
         ViewStatsController statsController, ViewStatsViewModel statsModel,
         Session session) {
  setLayout(new BorderLayout(8, 8));
  JPanel header = new JPanel(new GridLayout(0, 1));
  JButton refresh = new JButton("Refresh my stats and attendance history");
  JLabel summary = new JLabel(" ");
  JLabel error = new JLabel(" ");
  header.add(refresh); header.add(summary); header.add(error);
  add(header, BorderLayout.NORTH);
  DefaultTableModel model = new DefaultTableModel(
          new String[] {"Event", "Date", "Type", "Attendance", "Performance notes"}, 0) {
   @Override public boolean isCellEditable(int row, int col) { return false; }
  };
  add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
  Runnable load = () -> { historyController.execute(); statsController.execute(); };
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
   ViewStatsOutputData data = statsModel.getData();
   summary.setText(String.format("Present: %d  Absent: %d  Excused: %d  Rate: %.1f%%",
           data.getPresent(), data.getAbsent(), data.getExcused(),
           data.getAttendanceRate() * 100));
   if (!statsModel.getError().isEmpty()) { error.setText(statsModel.getError()); }
  });
  session.addPropertyChangeListener(e -> {
   model.setRowCount(0); summary.setText(" "); error.setText(" ");
   if (session.getRole() == entity.Role.PLAYER) { load.run(); }
  });
 }
}
