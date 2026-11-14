package view;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import interface_adapter.cancel_event.CancelEventController;
import interface_adapter.cancel_event.CancelEventViewModel;
import interface_adapter.edit_event.EditEventController;
import interface_adapter.edit_event.EditEventViewModel;
import interface_adapter.view_event_details.ViewEventDetailsController;
import interface_adapter.view_event_details.ViewEventDetailsViewModel;

/**
 * Details of one event, shown in a modal dialog over the calendar. The coach
 * of the event's team can also edit or cancel it from here.
 *
 * Plan.md describes this as a page, but the shell's pages belong to AppShell,
 * so a dialog keeps the calendar in place and needs no shell changes.
 * Closing the dialog runs the onClose callback so the calendar can reload.
 *
 * Three use cases share this view (view details, edit, cancel), each with its
 * own controller and view model. Whether the buttons show is only a
 * convenience; the edit and cancel interactors check permission themselves.
 */
public class EventDetailsView extends JPanel implements PropertyChangeListener {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Color INK = new Color(28, 32, 38);
    private static final Color INK_MUTED = new Color(130, 138, 148);
    private static final Color ERROR = new Color(217, 48, 37);
    private static final Color SUCCESS = new Color(22, 101, 52);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_VALUE = new Font("Segoe UI", Font.PLAIN, 13);

    private static final String READ_CARD = "read";
    private static final String EDIT_CARD = "edit";

    private final ViewEventDetailsController detailsController;
    private final ViewEventDetailsViewModel detailsViewModel;
    private EditEventController editController;
    private EditEventViewModel editViewModel;
    private CancelEventController cancelController;
    private CancelEventViewModel cancelViewModel;

    private final JLabel titleLabel = new JLabel(" ");
    private final JLabel cancelledLabel = new JLabel("CANCELLED");
    private final JLabel teamValue = valueLabel();
    private final JLabel dateValue = valueLabel();
    private final JLabel timeValue = valueLabel();
    private final JLabel locationValue = valueLabel();
    private final JLabel opponentCaption = captionLabel("Opponent");
    private final JLabel opponentValue = valueLabel();
    private final JLabel statusLabel = new JLabel(" ");

    private final JTextField dateTimeField = new JTextField(16);
    private final JTextField locationField = new JTextField(16);
    private final JTextField opponentField = new JTextField(16);

    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final JButton editButton = new JButton("Edit");
    private final JButton cancelEventButton = new JButton("Cancel event");
    private final JButton saveButton = new JButton("Save");
    private final JButton discardButton = new JButton("Discard");

    private JDialog dialog;
    private Runnable onClose = () -> { };
    private String currentEventId;
    private boolean editing;

    /**
     * Creates the view.
     *
     * @param detailsController loads the event to show
     * @param detailsViewModel  observed for the event's details
     */
    public EventDetailsView(ViewEventDetailsController detailsController,
                            ViewEventDetailsViewModel detailsViewModel) {
        this.detailsController = detailsController;
        this.detailsViewModel = detailsViewModel;
        detailsViewModel.addPropertyChangeListener(this);

        setLayout(new BorderLayout(0, 16));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 16, 24));

        final JPanel heading = new JPanel();
        heading.setOpaque(false);
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(INK);
        cancelledLabel.setFont(FONT_LABEL);
        cancelledLabel.setForeground(ERROR);
        heading.add(titleLabel);
        heading.add(cancelledLabel);
        add(heading, BorderLayout.NORTH);

        final JPanel rows = new JPanel(new GridLayout(0, 2, 12, 8));
        rows.setOpaque(false);
        rows.add(captionLabel("Team"));
        rows.add(teamValue);
        rows.add(captionLabel("Date"));
        rows.add(dateValue);
        rows.add(captionLabel("Time"));
        rows.add(timeValue);
        rows.add(captionLabel("Location"));
        rows.add(locationValue);
        rows.add(opponentCaption);
        rows.add(opponentValue);

        final JPanel form = new JPanel(new GridLayout(0, 2, 12, 8));
        form.setOpaque(false);
        form.add(captionLabel("Date/time (yyyy-MM-dd HH:mm)"));
        form.add(dateTimeField);
        form.add(captionLabel("Location"));
        form.add(locationField);
        form.add(captionLabel("Opponent (games only)"));
        form.add(opponentField);

        // Top-aligned wrappers so the shorter form doesn't stretch its rows to
        // the height of the details card.
        body.setOpaque(false);
        body.add(topAligned(rows), READ_CARD);
        body.add(topAligned(form), EDIT_CARD);
        add(body, BorderLayout.CENTER);

        final JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setOpaque(false);
        statusLabel.setFont(FONT_VALUE);
        footer.add(statusLabel, BorderLayout.NORTH);

        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        editButton.addActionListener(e -> startEditing());
        cancelEventButton.addActionListener(e -> onCancelEvent());
        saveButton.addActionListener(e -> onSave());
        discardButton.addActionListener(e -> stopEditing());
        final JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> close());
        buttons.add(editButton);
        buttons.add(cancelEventButton);
        buttons.add(saveButton);
        buttons.add(discardButton);
        buttons.add(closeButton);
        footer.add(buttons, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH);

        updateControls();
    }

    /**
     * Connects the Edit button to the Edit Event use case.
     *
     * @param controller saves the coach's changes
     * @param viewModel  observed for the result
     */
    public void setEditEventController(EditEventController controller,
                                       EditEventViewModel viewModel) {
        this.editController = controller;
        this.editViewModel = viewModel;
        viewModel.addPropertyChangeListener(this);
    }

    /**
     * Connects the Cancel event button to the Cancel Event use case.
     *
     * @param controller cancels the event
     * @param viewModel  observed for the result
     */
    public void setCancelEventController(CancelEventController controller,
                                         CancelEventViewModel viewModel) {
        this.cancelController = controller;
        this.cancelViewModel = viewModel;
        viewModel.addPropertyChangeListener(this);
    }

    /**
     * Sets what happens after the dialog closes, e.g. reloading the calendar.
     *
     * @param onClose callback run on the Swing thread after closing
     */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /**
     * Loads an event and shows it in a modal dialog. Returns once the dialog
     * is closed.
     *
     * @param eventId the event to show
     * @param parent  component the dialog is centred over
     */
    public void open(String eventId, Component parent) {
        currentEventId = eventId;
        editing = false;
        detailsController.execute(eventId);

        final Window owner = SwingUtilities.getWindowAncestor(parent);
        dialog = new JDialog(owner, "Event Details", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(this);
        dialog.pack();
        dialog.setMinimumSize(dialog.getSize());
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        // setVisible blocks until the dialog is closed, by button or window X.
        dialog = null;
        onClose.run();
    }

    private void close() {
        if (dialog != null) {
            dialog.dispose();
        }
    }

    private void startEditing() {
        dateTimeField.setText(detailsViewModel.getDateTime() == null
                ? "" : detailsViewModel.getDateTime().format(FORMAT));
        locationField.setText(detailsViewModel.getLocation());
        opponentField.setText(detailsViewModel.getOpponent());
        opponentField.setEnabled(detailsViewModel.isGame());
        editing = true;
        showStatus("", false);
        updateControls();
    }

    private void stopEditing() {
        editing = false;
        showStatus("", false);
        updateControls();
    }

    private void onSave() {
        final LocalDateTime dateTime;
        try {
            dateTime = LocalDateTime.parse(dateTimeField.getText().trim(), FORMAT);
        }
        catch (DateTimeParseException ex) {
            showStatus("Invalid date/time format.", true);
            return;
        }
        editController.execute(currentEventId, dateTime,
                locationField.getText(), opponentField.getText());
    }

    private void onCancelEvent() {
        final int answer = JOptionPane.showConfirmDialog(this,
                "Cancel " + detailsViewModel.getTitle() + " on " + detailsViewModel.getDate()
                        + "?\nIt will stay on the calendar, marked as cancelled.",
                "Cancel Event", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            cancelController.execute(currentEventId);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == editViewModel) {
            onEditResult();
        }
        else if (evt.getSource() == cancelViewModel) {
            onCancelResult();
        }
        else {
            showDetails();
        }
    }

    private void onEditResult() {
        if (!editViewModel.getErrorMessage().isEmpty()) {
            // Stay in the form so the coach can fix what they typed.
            showStatus(editViewModel.getErrorMessage(), true);
            return;
        }
        editing = false;
        // Reload from the store so the dialog shows what was saved, not what
        // was typed.
        detailsController.execute(currentEventId);
        showStatus(editViewModel.getSuccessMessage(), false);
    }

    private void onCancelResult() {
        if (!cancelViewModel.getErrorMessage().isEmpty()) {
            showStatus(cancelViewModel.getErrorMessage(), true);
            return;
        }
        detailsController.execute(currentEventId);
        showStatus(cancelViewModel.getSuccessMessage(), false);
    }

    private void showDetails() {
        final String error = detailsViewModel.getErrorMessage();
        if (!error.isEmpty()) {
            editing = false;
        }
        showStatus(error, true);

        titleLabel.setText(error.isEmpty() ? detailsViewModel.getTitle() : "Event unavailable");
        cancelledLabel.setVisible(detailsViewModel.isCancelled());
        teamValue.setText(detailsViewModel.getTeamName());
        dateValue.setText(detailsViewModel.getDate());
        timeValue.setText(detailsViewModel.getTime());
        locationValue.setText(detailsViewModel.getLocation());
        opponentValue.setText(detailsViewModel.getOpponent());
        // Practices have no opponent, so the row would only ever be blank.
        opponentCaption.setVisible(detailsViewModel.isGame());
        opponentValue.setVisible(detailsViewModel.isGame());

        updateControls();
    }

    /** Shows the right card and buttons for the current event and mode. */
    private void updateControls() {
        final boolean loaded = detailsViewModel.getErrorMessage().isEmpty()
                && !detailsViewModel.getEventId().isEmpty();
        final boolean manageable = loaded && detailsViewModel.isCanManage()
                && !detailsViewModel.isCancelled();

        editButton.setVisible(manageable && !editing && editController != null);
        cancelEventButton.setVisible(manageable && !editing && cancelController != null);
        saveButton.setVisible(editing);
        discardButton.setVisible(editing);
        cards.show(body, editing ? EDIT_CARD : READ_CARD);

        if (dialog != null) {
            dialog.pack();
        }
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message.isEmpty() ? " " : message);
        statusLabel.setForeground(isError ? ERROR : SUCCESS);
    }

    private static JPanel topAligned(JPanel inner) {
        final JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(inner, BorderLayout.NORTH);
        return wrapper;
    }

    private static JLabel captionLabel(String text) {
        final JLabel label = new JLabel(text);
        label.setFont(FONT_LABEL);
        label.setForeground(INK_MUTED);
        return label;
    }

    private static JLabel valueLabel() {
        final JLabel label = new JLabel(" ");
        label.setFont(FONT_VALUE);
        label.setForeground(INK);
        return label;
    }
}
