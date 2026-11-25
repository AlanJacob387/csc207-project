package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/**
 * Page where the user connects the app to the shared database, without a
 * terminal.
 *
 * The URL and the publishable key are typed once, tested against the database,
 * and saved into the user's home folder by whatever the caller supplies. This
 * page only collects text and shows messages; the saving and the connection
 * check are callbacks, so the page has no dependency on the data layer and can
 * be tested without a window.
 */
public class DatabaseSettingsView extends JPanel {

    /** Checks settings against the database without saving them. */
    @FunctionalInterface
    public interface ConnectionTester {

        /**
         * Checks two values.
         *
         * @param url the project URL
         * @param key the publishable key
         * @return whether they work, with a message to show
         */
        TestResult test(String url, String key);
    }

    /** Saves settings for this user. */
    @FunctionalInterface
    public interface SettingsSaver {

        /**
         * Saves two values.
         *
         * @param url the project URL
         * @param key the publishable key
         * @throws Exception when the values cannot be saved
         */
        void save(String url, String key) throws Exception;
    }

    /** The outcome of a check, as this page needs it. */
    public static final class TestResult {

        private final boolean ok;
        private final String message;

        /**
         * Creates the result.
         *
         * @param ok      whether the settings worked
         * @param message what to show
         */
        public TestResult(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        public boolean isOk() {
            return ok;
        }

        public String getMessage() {
            return message;
        }
    }

    private static final Color PROBLEM = new Color(180, 30, 30);
    private static final Color OK = new Color(22, 101, 52);
    private static final Color INK = new Color(90, 98, 110);
    private static final int FIELD_WIDTH = 380;

    private final JTextField urlField = new JTextField(30);
    private final JPasswordField keyField = new JPasswordField(30);
    private final JLabel statusLabel = new JLabel(" ");
    private final String settingsFile;

    private ConnectionTester tester;
    private SettingsSaver saver;
    private Runnable onBackToLogin;

    /**
     * Creates the page.
     *
     * @param currentUrl   the URL already configured, or an empty string
     * @param settingsFile where saving will put the settings, for the hint text
     */
    public DatabaseSettingsView(String currentUrl, String settingsFile) {
        this.settingsFile = settingsFile;
        urlField.setText(currentUrl == null ? "" : currentUrl);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(40, 56, 40, 56));
        add(buildHeader());
        add(Box.createVerticalStrut(24));
        add(buildForm());
        add(Box.createVerticalStrut(14));
        add(buildButtons());
        add(Box.createVerticalStrut(16));
        add(statusLabel);
    }

    /**
     * Gives the page the check to run when Test connection is pressed.
     *
     * @param tester the connection check
     */
    public void setOnTestConnection(ConnectionTester tester) {
        this.tester = tester;
    }

    /**
     * Gives the page the save to run when Save is pressed.
     *
     * @param saver the settings saver
     */
    public void setOnSave(SettingsSaver saver) {
        this.saver = saver;
    }

    /**
     * Sets what happens when the user goes back to the sign-in page.
     *
     * @param onBackToLogin the navigation callback
     */
    public void setOnBackToLogin(Runnable onBackToLogin) {
        this.onBackToLogin = onBackToLogin;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private JPanel buildHeader() {
        final JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Color.WHITE);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel title = new JLabel("Database connection");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel subtitle = new JLabel("<html>Paste the project URL and the publishable key from "
                + "Supabase<br>(Settings &rarr; API Keys). The key is stored on this computer only.</html>");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        subtitle.setForeground(INK);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(subtitle);
        return header;
    }

    private JPanel buildForm() {
        final JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);

        urlField.setMaximumSize(new Dimension(FIELD_WIDTH, 30));
        keyField.setMaximumSize(new Dimension(FIELD_WIDTH, 30));

        form.add(label("Project URL"));
        form.add(Box.createVerticalStrut(4));
        form.add(urlField);
        form.add(Box.createVerticalStrut(14));
        form.add(label("Publishable key"));
        form.add(Box.createVerticalStrut(4));
        form.add(keyField);
        return form;
    }

    private JPanel buildButtons() {
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        buttons.setBackground(Color.WHITE);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JButton testButton = new JButton("Test connection");
        testButton.addActionListener(e -> onTest());
        final JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> onSave());

        buttons.add(testButton);
        buttons.add(Box.createHorizontalStrut(10));
        buttons.add(saveButton);
        buttons.add(Box.createHorizontalStrut(20));
        buttons.add(link("Back to log in", () -> {
            if (onBackToLogin != null) {
                onBackToLogin.run();
            }
        }));
        return buttons;
    }

    private static JLabel label(String text) {
        final JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(INK);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JButton link(String text, Runnable action) {
        final JButton button = new JButton(text);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(new Color(30, 90, 200));
        button.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        return button;
    }

    // ------------------------------------------------------------------
    // Behaviour
    // ------------------------------------------------------------------

    /** Presses Test connection: checks the typed values without saving them. */
    void onTest() {
        if (tester == null) {
            show("Connection testing is not wired up in this preview.", PROBLEM);
            return;
        }
        final String url = urlField.getText().trim();
        final String key = new String(keyField.getPassword()).trim();
        if (url.isEmpty() || key.isEmpty()) {
            show("Enter both a project URL and a publishable key.", PROBLEM);
            return;
        }
        show("Testing...", INK);
        final TestResult result = tester.test(url, key);
        show(result.getMessage(), result.isOk() ? OK : PROBLEM);
    }

    /** Presses Save: stores the typed values for this user. */
    void onSave() {
        if (saver == null) {
            show("Saving is not wired up in this preview.", PROBLEM);
            return;
        }
        final String url = urlField.getText().trim();
        final String key = new String(keyField.getPassword()).trim();
        if (url.isEmpty() || key.isEmpty()) {
            show("Enter both a project URL and a publishable key.", PROBLEM);
            return;
        }
        try {
            saver.save(url, key);
            show("Saved to " + settingsFile + " - restart the app to connect.", OK);
        }
        catch (Exception ex) {
            show(ex.getMessage() == null ? ex.toString() : ex.getMessage(), PROBLEM);
        }
    }

    // ------------------------------------------------------------------
    // Test seams: package-private so tests can drive the page without a mouse.
    // ------------------------------------------------------------------

    /**
     * Types settings the way a user would.
     *
     * @param url the project URL
     * @param key the publishable key
     */
    void typeSettings(String url, String key) {
        urlField.setText(url);
        keyField.setText(key);
    }

    /**
     * Reads back the status line.
     *
     * @return the message currently shown
     */
    String status() {
        return statusLabel.getText();
    }

    /**
     * Reads back the URL field, so a test can check that the page starts with the
     * project already configured.
     *
     * @return the URL currently typed
     */
    String typedUrl() {
        return urlField.getText();
    }

    /** Presses Save the way a test would. */
    void clickSave() {
        onSave();
    }

    /** Presses Test connection the way a test would. */
    void clickTest() {
        onTest();
    }

    private void show(String message, Color colour) {
        statusLabel.setForeground(colour);
        statusLabel.setText(message);
    }
}
