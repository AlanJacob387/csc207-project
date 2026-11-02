package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import interface_adapter.change_password.ChangePasswordController;
import interface_adapter.change_password.ChangePasswordViewModel;

/**
 * Swing view for the Account page, where a signed-in user changes their own
 * password.
 *
 * The page only collects input and shows messages: the Change Password use case
 * decides whether the change is allowed, and it takes the username from the
 * current-user port rather than from this view.
 */
public class ChangePasswordView extends JPanel implements PropertyChangeListener {

    private static final int FIELD_WIDTH = 320;
    private static final int FIELD_HEIGHT = 30;
    private static final Color ERROR = new Color(180, 30, 30);
    private static final Color SUCCESS = new Color(22, 101, 52);
    private static final Color LABEL_INK = new Color(90, 98, 110);

    private final ChangePasswordViewModel viewModel;
    private ChangePasswordController controller;

    private final JPasswordField currentPasswordField = new JPasswordField(18);
    private final JPasswordField newPasswordField = new JPasswordField(18);
    private final JPasswordField repeatNewPasswordField = new JPasswordField(18);
    private final JLabel messageLabel = new JLabel(" ");

    /**
     * Creates the page.
     *
     * @param viewModel the account page state
     */
    public ChangePasswordView(ChangePasswordViewModel viewModel) {
        this.viewModel = viewModel;
        this.viewModel.addPropertyChangeListener(this);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(32, 48, 32, 48));
        add(buildContent());
    }

    /**
     * Gives the page the controller to call when the button is pressed.
     *
     * @param controller the change password controller
     */
    public void setChangePasswordController(ChangePasswordController controller) {
        this.controller = controller;
    }

    private JPanel buildContent() {
        final JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel title = new JLabel("Account");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel subtitle = new JLabel("Change your password");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(LABEL_INK);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JButton changeButton = new JButton("Change Password");
        changeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        changeButton.setPreferredSize(new Dimension(FIELD_WIDTH, 36));
        changeButton.addActionListener(e -> onChange());

        content.add(title);
        content.add(Box.createVerticalStrut(4));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(22));
        content.add(buildForm());
        content.add(Box.createVerticalStrut(14));
        content.add(messageLabel);
        content.add(Box.createVerticalStrut(14));
        content.add(changeButton);
        return content;
    }

    /** Lays the three password fields out with a small label above each. */
    private JPanel buildForm() {
        final JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setMaximumSize(new Dimension(FIELD_WIDTH, Integer.MAX_VALUE));

        int row = 0;
        row = addField(form, row, "Current password", currentPasswordField);
        row = addField(form, row, "New password", newPasswordField);
        addField(form, row, "Repeat new password", repeatNewPasswordField);
        return form;
    }

    /**
     * Adds one label and the field beneath it.
     *
     * @param form      the form panel
     * @param row       the grid row to start at
     * @param labelText the label above the field
     * @param field     the input component
     * @return the next free grid row
     */
    private static int addField(JPanel form, int row, String labelText, JComponent field) {
        final GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.weightx = 1;
        labelConstraints.insets = new Insets(row == 0 ? 0 : 10, 0, 3, 0);

        final JLabel label = new JLabel(labelText);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(LABEL_INK);
        form.add(label, labelConstraints);

        final GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 0;
        fieldConstraints.gridy = row + 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.weightx = 1;
        field.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        form.add(field, fieldConstraints);
        return row + 2;
    }

    private void onChange() {
        if (controller == null) {
            showMessage("Change password is not wired up in this preview.", ERROR);
            return;
        }
        messageLabel.setText(" ");
        controller.execute(
                new String(currentPasswordField.getPassword()),
                new String(newPasswordField.getPassword()),
                new String(repeatNewPasswordField.getPassword()));
    }

    private void showMessage(String text, Color colour) {
        messageLabel.setForeground(colour);
        messageLabel.setText(text);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final String error = viewModel.getErrorMessage();
        if (!error.isEmpty()) {
            showMessage(error, ERROR);
            return;
        }
        final String success = viewModel.getSuccessMessage();
        if (!success.isEmpty()) {
            showMessage(success, SUCCESS);
            currentPasswordField.setText("");
            newPasswordField.setText("");
            repeatNewPasswordField.setText("");
            return;
        }
        messageLabel.setText(" ");
    }
}