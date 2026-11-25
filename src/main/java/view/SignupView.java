package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import entity.Role;
import interface_adapter.signup.SignupController;
import interface_adapter.signup.SignupViewModel;

/**
 * Swing view for the Sign Up page.
 *
 * Shares the login page background photo (see {@link BackgroundImage}) and the
 * same white card styling, so the two pages feel like one app. The role dropdown
 * shows whatever roles the view model carries, and the model asks
 * {@link Role#selfAssignableNames()} - so this page holds no opinion about who
 * may sign up, and the use case refuses an administrative role even if something
 * bypasses the page.
 *
 * The view only collects input and shows messages: every decision is made by
 * the Sign Up use case.
 */
public class SignupView extends JPanel implements PropertyChangeListener {

    private static final int FIELD_WIDTH = 320;
    private static final int FIELD_HEIGHT = 30;
    private static final Color ACCENT = new Color(23, 66, 148);
    private static final Color ERROR = new Color(180, 30, 30);
    private static final Color LABEL_INK = new Color(90, 98, 110);

    private final SignupViewModel viewModel;
    private final BackgroundImage background = new BackgroundImage();
    private SignupController controller;
    private Runnable onBackToLogin;

    private final JTextField usernameField = new JTextField(18);
    private final JTextField displayNameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField repeatPasswordField = new JPasswordField(18);
    private final JComboBox<String> roleBox;
    private final JLabel messageLabel = new JLabel(" ");

    /**
     * Creates the page.
     *
     * @param viewModel the sign-up page state, including the roles it may offer
     */
    public SignupView(SignupViewModel viewModel) {
        this.viewModel = viewModel;
        this.roleBox = new JComboBox<>(viewModel.getRoleChoices().toArray(new String[0]));
        this.viewModel.addPropertyChangeListener(this);

        setLayout(new GridBagLayout());
        add(buildCard());
    }

    /**
     * The role the picker currently shows.
     *
     * A seam for the test that checks the default and that the picker follows the
     * view model, so the test does not have to reach into the component tree.
     *
     * @return the selected role name
     */
    String selectedRoleName() {
        return String.valueOf(roleBox.getSelectedItem());
    }

    /**
     * Gives the page the controller to call when Create Account is pressed.
     *
     * @param controller the signup controller
     */
    public void setSignupController(SignupController controller) {
        this.controller = controller;
    }

    /**
     * Sets what happens when the user asks to go back to the login page.
     *
     * @param onBackToLogin the navigation callback
     */
    public void setOnBackToLogin(Runnable onBackToLogin) {
        this.onBackToLogin = onBackToLogin;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        background.paint(g2, getWidth(), getHeight());
        g2.dispose();
    }

    // ------------------------------------------------------------------
    // Card
    // ------------------------------------------------------------------
    private JPanel buildCard() {
        final JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 60), 1, true),
                BorderFactory.createEmptyBorder(32, 48, 28, 48)));

        final JLabel title = new JLabel("Create an Account");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JLabel subtitle = new JLabel("Join your team''s platform");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        subtitle.setForeground(Color.DARK_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JPanel form = buildForm();

        messageLabel.setForeground(ERROR);
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JButton createButton = new JButton("Create Account");
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setMaximumSize(new Dimension(FIELD_WIDTH, 38));
        createButton.setPreferredSize(new Dimension(FIELD_WIDTH, 38));
        createButton.addActionListener(e -> onCreate());

        final JButton backButton = new JButton("Back to Log In");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setFocusPainted(false);
        backButton.setForeground(ACCENT);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> onBack());

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(24));
        card.add(form);
        card.add(Box.createVerticalStrut(12));
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(createButton);
        card.add(Box.createVerticalStrut(4));
        card.add(backButton);
        return card;
    }

    /** Lays the fields out in one column, each with a small label above it. */
    private JPanel buildForm() {
        final JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.setMaximumSize(new Dimension(FIELD_WIDTH, Integer.MAX_VALUE));

        roleBox.setSelectedItem(Role.PLAYER.name());

        int row = 0;
        row = addField(form, row, "Username", usernameField);
        row = addField(form, row, "Display name (optional)", displayNameField);
        row = addField(form, row, "Password", passwordField);
        row = addField(form, row, "Repeat password", repeatPasswordField);
        addField(form, row, "I am a...", roleBox);
        return form;
    }

    /**
     * Adds one small label and the field beneath it.
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

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------
    private void onCreate() {
        if (controller == null) {
            showMessage("Sign up is not wired up in this preview.", ERROR);
            return;
        }
        messageLabel.setText(" ");
        controller.execute(
                usernameField.getText().trim(),
                new String(passwordField.getPassword()),
                new String(repeatPasswordField.getPassword()),
                displayNameField.getText().trim(),
                String.valueOf(roleBox.getSelectedItem()));
    }

    private void onBack() {
        messageLabel.setText(" ");
        if (onBackToLogin != null) {
            onBackToLogin.run();
        }
    }

    private void showMessage(String text, Color colour) {
        messageLabel.setForeground(colour);
        messageLabel.setText(text);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final String error = viewModel.getErrorMessage();
        if (error.isEmpty()) {
            messageLabel.setText(" ");
        }
        else {
            showMessage(error, ERROR);
        }
    }
}
