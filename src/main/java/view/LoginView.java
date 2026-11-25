package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import interface_adapter.login.LoginController;
import interface_adapter.login.LoginViewModel;

/**
 * Swing view for the Login page.
 *
 * Full-screen page with the shared stadium photo background (see
 * {@link BackgroundImage}) and a centered white login card.
 *
 * The Log In button calls the LoginController, and the page shows whatever
 * message the LoginViewModel holds; navigation is decided by the presenter,
 * so this view never switches pages itself.
 */
public class LoginView extends JPanel implements PropertyChangeListener {

    private final BackgroundImage background = new BackgroundImage();

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");

    private final LoginViewModel viewModel;
    private LoginController controller;
    private Runnable onGoToSignup;
    private Runnable onOpenDatabaseSettings;

    private final JLabel storeLabel = new JLabel(" ");

    /** Creates the page with a fresh view model. */
    public LoginView() {
        this(new LoginViewModel());
    }

    /**
     * Creates the page with the view model it observes.
     *
     * @param viewModel the login page state
     */
    public LoginView(LoginViewModel viewModel) {
        this.viewModel = viewModel;
        this.viewModel.addPropertyChangeListener(this);

        // GridBagLayout keeps the login card centered at any window size.
        setLayout(new GridBagLayout());
        add(buildLoginCard());
    }

    // ------------------------------------------------------------------
    // Background painting
    // ------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        background.paint(g2, getWidth(), getHeight());


        g2.dispose();
    }

    // ------------------------------------------------------------------
    // Centered login card
    // ------------------------------------------------------------------
    private JPanel buildLoginCard() {
        final JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 60), 1, true),
                BorderFactory.createEmptyBorder(35, 50, 35, 50)));

        final JLabel title = new JLabel("Team Sports Management");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        subtitle.setForeground(Color.DARK_GRAY);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JLabel usernameLabel = new JLabel("Username");
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(300, 32));

        final JLabel passwordLabel = new JLabel("Password");
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(300, 32));

        errorLabel.setForeground(new Color(180, 30, 30));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JButton loginButton = new JButton("Log In");
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(300, 38));

        final JButton signupButton = new JButton("Create an Account");
        signupButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        signupButton.setMaximumSize(new Dimension(300, 38));

        loginButton.addActionListener(e -> onLogin());
        signupButton.addActionListener(e -> onGoToSignup());

        storeLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        storeLabel.setForeground(new Color(120, 128, 140));
        storeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        final JButton settingsLink = new JButton("Database settings");
        settingsLink.setBorder(BorderFactory.createEmptyBorder());
        settingsLink.setContentAreaFilled(false);
        settingsLink.setFocusPainted(false);
        settingsLink.setForeground(new Color(30, 90, 200));
        settingsLink.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        settingsLink.setAlignmentX(Component.CENTER_ALIGNMENT);
        settingsLink.addActionListener(e -> {
            if (onOpenDatabaseSettings != null) {
                onOpenDatabaseSettings.run();
            }
        });
        // Pressing Enter in the password field triggers login
        passwordField.addActionListener(e -> onLogin());

        card.add(title);
        card.add(Box.createVerticalStrut(5));
        card.add(subtitle);
        card.add(Box.createVerticalStrut(25));
        card.add(usernameLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(15));
        card.add(passwordLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(10));
        card.add(errorLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(10));
        card.add(signupButton);
        card.add(Box.createVerticalStrut(16));
        card.add(storeLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(settingsLink);
        return card;
    }

    /**
     * Gives the page the controller to call when Log In is pressed.
     *
     * @param controller the login controller
     */
    public void setLoginController(LoginController controller) {
        this.controller = controller;
    }

    /**
     * Sets what happens when the user asks for the sign-up page.
     *
     * @param onGoToSignup the navigation callback
     */
    public void setOnGoToSignup(Runnable onGoToSignup) {
        this.onGoToSignup = onGoToSignup;
    }

    /**
     * Sets what happens when the user opens the database settings.
     *
     * @param onOpenDatabaseSettings the navigation callback
     */
    public void setOnOpenDatabaseSettings(Runnable onOpenDatabaseSettings) {
        this.onOpenDatabaseSettings = onOpenDatabaseSettings;
    }

    /**
     * Shows which store the app is using, so nobody has to guess whether they are
     * on the shared database or on a local file.
     *
     * @param description the store description, from AppDataStore.describe()
     */
    public void setStoreDescription(String description) {
        storeLabel.setText(description == null || description.isBlank() ? " " : description);
    }

    /**
     * Reads back the store description, for tests.
     *
     * @return the text under the buttons
     */
    String storeDescription() {
        return storeLabel.getText();
    }

    /**
     * Shows a problem that happened before this page could open, such as a store
     * that could not be reached while the app was starting. It is shown in the
     * message line, so the user learns why signing in will fail.
     *
     * @param problem what to tell the user; blank clears the line
     */
    public void setStartupProblem(String problem) {
        errorLabel.setForeground(new Color(180, 30, 30));
        errorLabel.setText(problem == null || problem.isBlank() ? " " : problem);
    }

    /**
     * Reads back the message line, so a test can check the startup problem.
     *
     * @return the text currently shown in the message line
     */
    String startupProblem() {
        return errorLabel.getText();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        final String error = viewModel.getErrorMessage();
        if (!error.isEmpty()) {
            errorLabel.setForeground(new Color(180, 30, 30));
            errorLabel.setText(error);
            return;
        }
        final String success = viewModel.getSuccessMessage();
        if (!success.isEmpty()) {
            errorLabel.setForeground(new Color(22, 101, 52));
            errorLabel.setText(success);
            return;
        }
        errorLabel.setText(" ");
    }

    private void onLogin() {
        final String username = usernameField.getText().trim();
        final String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        errorLabel.setText(" ");
        if (controller == null) {
            errorLabel.setForeground(new Color(180, 30, 30));
            errorLabel.setText("Login is not wired up in this preview.");
            return;
        }
        controller.execute(username, password);
        passwordField.setText("");
    }

    private void onGoToSignup() {
        errorLabel.setText(" ");
        if (onGoToSignup != null) {
            onGoToSignup.run();
        }
    }

    /** Standalone preview — run this class directly to see the page. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            final JFrame frame = new JFrame("Login — Team Sports Management");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new LoginView());
            // Open maximized (full screen)
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            frame.setMinimumSize(new Dimension(900, 600));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}