package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.HierarchyEvent;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import entity.Role;
import interface_adapter.Session;
import interface_adapter.submit_absence_notice.SubmitAbsenceNoticeController;
import interface_adapter.submit_absence_notice.SubmitAbsenceNoticeViewModel;
import interface_adapter.view_absence_notices.ViewAbsenceNoticesController;
import interface_adapter.view_absence_notices.ViewAbsenceNoticesViewModel;
import use_case.view_absence_notices.ViewAbsenceNoticesOutputData;

/** A linked-child absence form for parents and an authorized notice feed for coaches. */
public class AbsenceNoticesView extends JPanel {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");
    private final SubmitAbsenceNoticeController submitController;
    private final SubmitAbsenceNoticeViewModel submitViewModel;
    private final ViewAbsenceNoticesController viewController;
    private final ViewAbsenceNoticesViewModel viewModel;
    private final Session session;
    private final JComboBox<ChildOption> children = new JComboBox<>();
    private final JComboBox<EventOption> events = new JComboBox<>();
    private final JTextArea reason = new JTextArea(3, 40);
    private final JTextArea feed = new JTextArea();
    private final JLabel loadFeedback = new JLabel(" ");
    private final JLabel submitFeedback = new JLabel(" ");
    private final JPanel childControls = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private final JPanel composer = new JPanel(new BorderLayout(0, 8));
    private final JButton submit = new JButton("Save absence notice");
    private boolean updating;

    public AbsenceNoticesView(SubmitAbsenceNoticeController submitController,
                              SubmitAbsenceNoticeViewModel submitViewModel,
                              ViewAbsenceNoticesController viewController,
                              ViewAbsenceNoticesViewModel viewModel, Session session) {
        this.submitController = submitController;
        this.submitViewModel = submitViewModel;
        this.viewController = viewController;
        this.viewModel = viewModel;
        this.session = session;
        setLayout(new BorderLayout(0, 14));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));
        add(buildHeader(), BorderLayout.NORTH);
        feed.setName("absenceFeed");
        feed.setEditable(false);
        feed.setLineWrap(true);
        feed.setWrapStyleWord(true);
        feed.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        feed.getAccessibleContext().setAccessibleName("Absence notice history");
        add(new JScrollPane(feed), BorderLayout.CENTER);
        add(buildComposer(), BorderLayout.SOUTH);
        viewModel.addPropertyChangeListener(event -> render());
        submitViewModel.addPropertyChangeListener(event -> renderSubmission());
        session.addPropertyChangeListener(event -> resetForSession());
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                refresh();
            }
        });
        resetForSession();
    }

    private JPanel buildHeader() {
        final JPanel header = new JPanel(new BorderLayout(0, 10));
        header.setOpaque(false);
        final JLabel title = new JLabel("Absence notices");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
        header.add(title, BorderLayout.NORTH);
        final JPanel controls = new JPanel(new BorderLayout());
        controls.setOpaque(false);
        childControls.setOpaque(false);
        children.setName("absenceChild");
        children.setPreferredSize(new Dimension(260, 30));
        final JLabel childLabel = new JLabel("Child");
        childLabel.setLabelFor(children);
        childControls.add(childLabel);
        childControls.add(children);
        children.addActionListener(event -> {
            if (!updating) {
                reason.setText("");
                submitViewModel.clear();
                refresh();
            }
        });
        controls.add(childControls, BorderLayout.WEST);
        final JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        controls.add(refresh, BorderLayout.EAST);
        header.add(controls, BorderLayout.CENTER);
        loadFeedback.putClientProperty("html.disable", true);
        header.add(loadFeedback, BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildComposer() {
        composer.setOpaque(false);
        final JPanel eventRow = new JPanel(new BorderLayout(8, 0));
        eventRow.setOpaque(false);
        final JLabel label = new JLabel("Upcoming event");
        label.setLabelFor(events);
        events.setName("absenceEvent");
        eventRow.add(label, BorderLayout.WEST);
        eventRow.add(events, BorderLayout.CENTER);
        composer.add(eventRow, BorderLayout.NORTH);
        final JPanel reasonPanel = new JPanel(new BorderLayout(0, 4));
        reasonPanel.setOpaque(false);
        final JLabel reasonLabel = new JLabel("Reason for absence");
        reasonLabel.setLabelFor(reason);
        reason.setName("absenceReason");
        reason.setLineWrap(true);
        reason.setWrapStyleWord(true);
        reasonPanel.add(reasonLabel, BorderLayout.NORTH);
        reasonPanel.add(new JScrollPane(reason), BorderLayout.CENTER);
        composer.add(reasonPanel, BorderLayout.CENTER);
        final JPanel actions = new JPanel(new BorderLayout(8, 0));
        actions.setOpaque(false);
        submitFeedback.putClientProperty("html.disable", true);
        actions.add(submitFeedback, BorderLayout.CENTER);
        submit.setName("absenceSubmit");
        submit.addActionListener(event -> {
            final EventOption choice = (EventOption) events.getSelectedItem();
            submitController.execute(selectedChild(), choice == null ? null : choice.row().eventId(), reason.getText());
        });
        actions.add(submit, BorderLayout.EAST);
        composer.add(actions, BorderLayout.SOUTH);
        return composer;
    }

    private void refresh() {
        if (session.isLoggedIn() && (session.getRole() == Role.PARENT || session.getRole() == Role.COACH)) {
            viewController.execute(selectedChild());
        }
    }

    private String selectedChild() {
        final ChildOption option = (ChildOption) children.getSelectedItem();
        return option == null ? null : option.row().username();
    }

    private void resetForSession() {
        reason.setText("");
        submitViewModel.clear();
        viewModel.clear();
        childControls.setVisible(session.getRole() == Role.PARENT);
        composer.setVisible(session.getRole() == Role.PARENT);
        refresh();
    }

    private void render() {
        final ViewAbsenceNoticesOutputData data = viewModel.getData();
        final EventOption previousEvent = (EventOption) events.getSelectedItem();
        updating = true;
        children.removeAllItems();
        events.removeAllItems();
        if (data != null) {
            for (var child : data.children()) {
                final ChildOption option = new ChildOption(child);
                children.addItem(option);
                if (child.username().equals(data.selectedPlayerUsername())) {
                    children.setSelectedItem(option);
                }
            }
            for (var event : data.events()) {
                final EventOption option = new EventOption(event);
                events.addItem(option);
                if (previousEvent != null && previousEvent.row().eventId().equals(event.eventId())) {
                    events.setSelectedItem(option);
                }
            }
        }
        updating = false;
        submit.setEnabled(data != null && data.canSubmit() && events.getItemCount() > 0);
        if (data == null) {
            feed.setText("");
            loadFeedback.setText(viewModel.getErrorMessage().isEmpty() ? " " : viewModel.getErrorMessage());
            return;
        }
        loadFeedback.setText(data.canSubmit() && data.children().isEmpty()
                ? "No linked players. Ask an administrator to link your child."
                : data.canSubmit() && data.events().isEmpty() ? "No upcoming active events for this child." : " ");
        final StringBuilder text = new StringBuilder();
        for (var notice : data.notices()) {
            text.append(notice.playerName()).append(" (").append(notice.playerUsername()).append(") — ")
                    .append(notice.teamName()).append(" ").append(notice.eventType()).append('\n')
                    .append(notice.dateTime() == null ? "Date unavailable" : TIME.format(notice.dateTime()))
                    .append(" · ").append(notice.location()).append('\n')
                    .append("From ").append(notice.parentName()).append(" (").append(notice.parentUsername())
                    .append(") · ").append(TIME.format(notice.submittedAt())).append('\n')
                    .append(notice.reason()).append("\n\n");
        }
        feed.setText(text.length() == 0 ? "No absence notices yet." : text.toString());
        feed.setCaretPosition(0);
    }

    private void renderSubmission() {
        final var result = submitViewModel.getData();
        submitFeedback.setText(result == null ? submitViewModel.getErrorMessage() : result.message());
        if (result != null) {
            reason.setText("");
            refresh();
        }
    }

    private record ChildOption(ViewAbsenceNoticesOutputData.ChildRow row) {
        @Override
        public String toString() {
            return row.displayName() + " (" + row.username() + ")";
        }
    }

    private record EventOption(ViewAbsenceNoticesOutputData.EventRow row) {
        @Override
        public String toString() {
            return row.teamName() + " · " + row.eventType() + " · " + TIME.format(row.dateTime())
                    + " · " + row.location();
        }
    }
}
