package view;
import java.awt.BorderLayout;
import javax.swing.*;
import entity.RsvpRecord;
import interface_adapter.list_available_events.*;
import use_case.list_available_events.AvailableEvent;
import interface_adapter.Session;
import interface_adapter.rsvp.*;
/** Event RSVP form; coaches can list player responses. */
public final class RsvpView extends JPanel {
 public RsvpView(RsvpController controller, RsvpViewModel vm, Session session,
         ListAvailableEventsController listController, ListAvailableEventsViewModel eventVm) {
  setLayout(new BorderLayout());
  JPanel form = new JPanel();
  JTextField eventId = new JTextField(14);
  JComboBox<RsvpRecord.Response> response = new JComboBox<>(RsvpRecord.Response.values());
  JButton send = new JButton("Save RSVP");
  JButton list = new JButton("Show team responses");
  form.add(new JLabel("Event ID")); form.add(eventId);
  JComboBox<AvailableEvent> events = new JComboBox<>();
  JButton refresh = new JButton("My events");
  refresh.addActionListener(e -> listController.execute());
  eventVm.addPropertyChangeListener(e -> {
   events.removeAllItems();
   for (AvailableEvent item : eventVm.getEvents()) { events.addItem(item); }
  });
  events.addActionListener(e -> {
   AvailableEvent chosen = (AvailableEvent) events.getSelectedItem();
   if (chosen != null) { eventId.setText(chosen.getEventId()); }
  });
  form.add(refresh); form.add(events);
  form.add(response); form.add(send); form.add(list);
  add(form, BorderLayout.NORTH);
  JTextArea display = new JTextArea(15, 42);
  display.setEditable(false);
  add(new JScrollPane(display), BorderLayout.CENTER);
  send.addActionListener(e -> controller.execute(eventId.getText().trim(),
          (RsvpRecord.Response) response.getSelectedItem()));
  list.addActionListener(e -> controller.execute(eventId.getText().trim(), null));
  vm.addPropertyChangeListener(e -> display.setText(vm.getMessage() + "\n"
          + String.join("\n", vm.getResponses())));
  session.addPropertyChangeListener(e -> { eventId.setText(""); display.setText(""); });
 }
}
