package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.HierarchyEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import entity.Role;
import entity.RsvpRecord;
import interface_adapter.Session;
import interface_adapter.list_available_events.ListAvailableEventsController;
import interface_adapter.list_available_events.ListAvailableEventsViewModel;
import interface_adapter.rsvp.RsvpController;
import interface_adapter.rsvp.RsvpViewModel;
import use_case.list_available_events.AvailableEvent;

/** Players answer YES or NO; coaches can only read their team's responses. */
public final class RsvpView extends JPanel {
    private final JComboBox<AvailableEvent> events = new JComboBox<>();
    private final JTextArea responses = new JTextArea(8, 36);
    private final JLabel feedback = new JLabel(" ");

    public RsvpView(RsvpController controller, RsvpViewModel vm, Session session,
            ListAvailableEventsController listController, ListAvailableEventsViewModel eventVm) {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        final JPanel form = L2FormStyles.page("Event RSVP",
                "Players confirm attendance; coaches see their team's responses.");
        events.setPreferredSize(new Dimension(640, 34));
        L2FormStyles.field(form, "Team event", events);

        // MAYBE remains in the legacy enum for loading old saved data, but cannot be submitted.
        final JComboBox<RsvpRecord.Response> response = new JComboBox<>(
                new RsvpRecord.Response[] {RsvpRecord.Response.YES, RsvpRecord.Response.NO});
        final JPanel playerControls = new JPanel();
        playerControls.setLayout(new javax.swing.BoxLayout(playerControls,
                javax.swing.BoxLayout.Y_AXIS));
        playerControls.setBackground(Color.WHITE);
        playerControls.setAlignmentX(LEFT_ALIGNMENT);
        L2FormStyles.field(playerControls, "Will you attend?", response);
        final JButton submit = new JButton("Save my RSVP");
        submit.setAlignmentX(LEFT_ALIGNMENT);
        playerControls.add(submit);
        playerControls.add(Box.createVerticalStrut(12));
        form.add(playerControls);

        final JLabel responseHeading = new JLabel("Player responses");
        responseHeading.setAlignmentX(LEFT_ALIGNMENT);
        form.add(responseHeading);
        form.add(Box.createVerticalStrut(6));
        responses.setEditable(false);
        responses.setLineWrap(true);
        responses.setWrapStyleWord(true);
        final JScrollPane responseScroll = new JScrollPane(responses);
        responseScroll.setPreferredSize(new Dimension(650, 180));
        responseScroll.setAlignmentX(LEFT_ALIGNMENT);
        responseScroll.setMaximumSize(new Dimension(L2FormStyles.FORM_WIDTH, 180));
        form.add(responseScroll);
        form.add(Box.createVerticalStrut(14));
        feedback.setAlignmentX(LEFT_ALIGNMENT);
        form.add(feedback);
        add(L2FormStyles.leftAligned(form), BorderLayout.CENTER);

        final Runnable applyRole = () -> {
            final boolean coach = session.getRole() == Role.COACH;
            playerControls.setVisible(session.getRole() == Role.PLAYER);
            responseHeading.setVisible(coach);
            responseScroll.setVisible(coach);
        };
        applyRole.run();

        submit.addActionListener(e -> {
            final AvailableEvent selected = (AvailableEvent) events.getSelectedItem();
            if (selected == null) { feedback.setText("Choose an event first."); return; }
            controller.execute(selected.getEventId(),
                    (RsvpRecord.Response) response.getSelectedItem());
        });
        // A coach never submits an RSVP. Selecting an event loads its players' answers.
        events.addActionListener(e -> {
            responses.setText("");
            final AvailableEvent selected = (AvailableEvent) events.getSelectedItem();
            if (selected != null && session.getRole() == Role.COACH) {
                controller.execute(selected.getEventId(), null);
            }
        });
        eventVm.addPropertyChangeListener(e -> {
            final AvailableEvent previous = (AvailableEvent) events.getSelectedItem();
            events.removeAllItems();
            for (AvailableEvent item : eventVm.getEvents()) {
                events.addItem(item);
                if (previous != null && item.getEventId().equals(previous.getEventId())) {
                    events.setSelectedItem(item);
                }
            }
            if (!eventVm.getError().isEmpty()) { feedback.setText(eventVm.getError()); }
        });
        vm.addPropertyChangeListener(e -> {
            feedback.setText(vm.getMessage());
            responses.setText(String.join("\n", vm.getResponses()));
        });
        session.addPropertyChangeListener(e -> {
            events.removeAllItems();
            responses.setText("");
            feedback.setText(" ");
            applyRole.run();
            if (isShowing()) { listController.execute(); }
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                listController.execute();
            }
        });
    }
}
