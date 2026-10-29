package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * A centered white card on the same dark gradient the login page falls back to,
 * with the fields needed to create an account. The role dropdown lets the team
 * demo every role without admin tooling; a real deployment would not let new
 * accounts grant themselves ADMIN.
 *
 * The view only collects input and shows messages: all the decisions happen in
 * the Sign Up use case.
 */
public class SignupView extends JPanel implements PropertyChangeListener {

    private final SignupViewModel viewModel;
    private SignupController controller;
    private Runnable onBackToLogin;

    private final JTextField usernameField = new JTextField(20);
    private final JTextField displayNameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField repeatPasswordField = new JPasswordField(20);
    private final JComboBox<String> roleBox = new JComboBox<>(roleNames());
    private final JLabel messageLabel = new JLabel(" ");

    /**
     * Creates the page.
     *
     * @param viewModel the sign-up page state
     */
    public SignupView(SignupViewModel viewModel) {
        this.viewModel = viewModel;
        this.viewModel.addPropertyChangeListener(this);

        setLayout(new GridBagLayout());
        add(buildCard());
    }

    /** Builds the role choices from the Role enum, so the two cannot drift apart. */
    private static String[] roleNames() {
        final Role[] roles = Role.values();
        final String[] names = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            names[i] = roles[i].name();
        }
        return names;
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
        g2.setPaint(new GradientPaint(0, 0, new Color(10, 25, 60),
                0, getHeight(), new Color(20, 90, 55)));
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
    }

    private JPanel buildCard() {
        final JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 60), 1, true),
                BorderFactory.createEmptyBorder(30, 45, 30, 45)));

        final JLabel title = new JLabel("Create an Account");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JLabel subtitle = new JLabel("Join your team's platform");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(Color.DARK_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        usernameField.setMaximumSize(new Dimension(300, 30));
        displayNameField.setMaximumSize(new Dimension(300, 30));
        passwordField.setMaximumSize(new Dimension(300, 30));
        repeatPasswordField.setMaximumSize(new Dimension(300, 30));
        roleBox.setMaximumSize(new Dimension(300, 30));
        roleBox.setSelectedItem(Role.PLAYER.name());

        messageLabel.setForeground(new Color(180, 30, 30));
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JButton createButton = new JButton("Create Account");
        createButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        createButton.setMaximumSize(new Dimension(300, 36));
        createButton.addActionListener(e -> onCreate());

        final JButton backButton = new JButton("Back to Log In");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setMaximumSize(new Dimension(300, 36));
        backButton.addActionListener(e -> onBack());

        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));
        card.add(fieldLabel("Username"));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("Display name (optional)"));
        card.add(displayNameField);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("Password"));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("Repeat password"));
        card.add(repeatPasswordField);
        card.add(Box.createVerticalStrut(10));
        card.add(fieldLabel("I am a..."));
        card.add(roleBox);
        card.add(Box.createVerticalStrut(10));
        card.add(messageLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(createButton);
        card.add(Box.createVerticalStrut(8));
        card.add(backButton);
        return card;
    }

    /** Builds a centered field label. */
    private static JLabel fieldLabel(String text) {
        final JLabel label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private void onCreate() {
        if (controller == null) {
            messageLabel.setForeground(new Color(180, 30, 30));
            messageLabel.setText("Sign up is not wired up in this preview.");
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

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final String error = viewModel.getErrorMessage();
        if (error.isEmpty()) {
            messageLabel.setText(" ");
        }
        else {
            messageLabel.setForeground(new Color(180, 30, 30));
            messageLabel.setText(error);
        }
    }
}
