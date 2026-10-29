package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.RenderingHints;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
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
 * Full-screen page with a stadium photo background
 * (src/main/resources/images/login_stadium.jpg) and a centered
 * white login card. The image is scaled to cover the whole window
 * while preserving its aspect ratio. If the image is missing, a
 * plain dark-green gradient is painted instead.
 *
 * The Log In button calls the LoginController, and the page shows whatever
 * message the LoginViewModel holds; navigation is decided by the presenter,
 * so this view never switches pages itself.
 */
public class LoginView extends JPanel implements PropertyChangeListener {

    /** Classpath location of the background image (under src/main/resources). */
    private static final String BACKGROUND_RESOURCE = "/images/login_stadium.jpg";

    private final Image backgroundImage;

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JLabel errorLabel = new JLabel(" ");

    private final LoginViewModel viewModel;
    private LoginController controller;
    private Runnable onGoToSignup;

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
        this.backgroundImage = loadBackgroundImage();

        // GridBagLayout keeps the login card centered at any window size.
        setLayout(new GridBagLayout());
        add(buildLoginCard());
    }

    private static Image loadBackgroundImage() {
        final URL url = LoginView.class.getResource(BACKGROUND_RESOURCE);
        if (url == null) {
            System.err.println("Background image not found on classpath: " + BACKGROUND_RESOURCE);
            return null;
        }
        try {
            return ImageIO.read(url);
        } catch (IOException e) {
            System.err.println("Failed to load background image: " + e.getMessage());
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Background painting
    // ------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();
        final int w = getWidth();
        final int h = getHeight();

        if (backgroundImage != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // Scale to cover: fill the whole panel, keep aspect ratio, crop overflow.
            final int imgW = backgroundImage.getWidth(this);
            final int imgH = backgroundImage.getHeight(this);
            final double scale = Math.max((double) w / imgW, (double) h / imgH);
            final int drawW = (int) Math.ceil(imgW * scale);
            final int drawH = (int) Math.ceil(imgH * scale);
            final int x = (w - drawW) / 2;
            final int y = (h - drawH) / 2;
            g2.drawImage(backgroundImage, x, y, drawW, drawH, this);

            // Subtle dark overlay so the white card stands out on bright photos.
            g2.setColor(new Color(0, 0, 0, 70));
            g2.fillRect(0, 0, w, h);
        } else {
            // Fallback: dark stadium-green gradient.
            g2.setPaint(new GradientPaint(0, 0, new Color(10, 25, 60),
                    0, h, new Color(20, 90, 55)));
            g2.fillRect(0, 0, w, h);
        }

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