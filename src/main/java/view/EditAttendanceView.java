package view;
import java.awt.BorderLayout;
import javax.swing.*;
import entity.AttendanceRecord;
import interface_adapter.list_available_events.*;
import use_case.list_available_events.AvailableEvent;
import interface_adapter.Session;
import interface_adapter.edit_attendance.*;
/** Coach's correction form for previously recorded attendance. */
public final class EditAttendanceView extends JPanel {
 public EditAttendanceView(EditAttendanceController controller, EditAttendanceViewModel vm, Session session,
         ListAvailableEventsController listController, ListAvailableEventsViewModel eventVm) {
  setLayout(new BorderLayout());
  JPanel form = new JPanel();
  JTextField event = new JTextField(12);
  JTextField player = new JTextField(12);
  JComboBox<AttendanceRecord.Status> status =
          new JComboBox<>(AttendanceRecord.Status.values());
  JTextField notes = new JTextField(22);
  JButton save = new JButton("Correct attendance / save notes");
  form.add(new JLabel("Event ID")); form.add(event);
  JComboBox<AvailableEvent> events = new JComboBox<>();
  JButton refresh = new JButton("My events");
  refresh.addActionListener(e -> listController.execute());
  eventVm.addPropertyChangeListener(e -> {
   events.removeAllItems();
   for (AvailableEvent item : eventVm.getEvents()) { events.addItem(item); }
  });
  events.addActionListener(e -> {
   AvailableEvent chosen = (AvailableEvent) events.getSelectedItem();
   if (chosen != null) { event.setText(chosen.getEventId()); }
  });
  form.add(refresh); form.add(events);
  form.add(new JLabel("Player username")); form.add(player);
  form.add(status); form.add(new JLabel("Performance notes")); form.add(notes); form.add(save);
  add(form, BorderLayout.NORTH);
  JLabel feedback = new JLabel(" ");
  add(feedback, BorderLayout.CENTER);
  save.addActionListener(e -> controller.execute(event.getText().trim(),
          player.getText().trim(), (AttendanceRecord.Status) status.getSelectedItem(),
          notes.getText().trim()));
  vm.addPropertyChangeListener(e -> feedback.setText(vm.getMessage()));
  session.addPropertyChangeListener(e -> {
   event.setText(""); player.setText(""); notes.setText(""); feedback.setText(" ");
  });
 }
}
