package view;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import entity.Role;
import interface_adapter.Session;
import interface_adapter.manage_users.ChangeUserRoleController;
import interface_adapter.manage_users.CreateUserController;
import interface_adapter.manage_users.DeactivateUserController;
import interface_adapter.manage_users.ListAccountsController;
import interface_adapter.manage_users.ManageUsersViewModel;
import interface_adapter.manage_users.ReactivateUserController;
import use_case.manage_users.ManageUsersOutputData.AccountData;

/**
 * Administrator page: every account on the platform, plus the read and the four
 * actions.
 *
 * The page only collects input and shows messages; who may use it, which role may
 * be assigned and whether an account can be deactivated or reactivated are all
 * decided by the interactors. An account is never deleted - deactivating keeps the
 * history that events, announcements and attendance point at, and reactivating
 * undoes it.
 */
public class ManageUsersView extends JPanel implements PropertyChangeListener {

    private static final Color PROBLEM = new Color(180, 30, 30);
    private static final Color OK = new Color(22, 101, 52);
    private static final Color INK = new Color(90, 98, 110);
    private static final int FIELD_WIDTH = 240;

    private final ListAccountsController listAccountsController;
    private final CreateUserController createUserController;
    private final ChangeUserRoleController changeUserRoleController;
    private final DeactivateUserController deactivateUserController;
    private final ReactivateUserController reactivateUserController;
    private final ManageUsersViewModel viewModel;
    private final Session session;

    private final JTextArea accountList = new JTextArea(7, 52);
    private final JTextField usernameField = new JTextField(16);
    private final JTextField displayNameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JPasswordField repeatPasswordField = new JPasswordField(16);
    private final JComboBox<String> newRoleBox = new JComboBox<>(roleNames());
    private final JComboBox<String> accountBox = new JComboBox<>();
    private final JComboBox<String> roleBox = new JComboBox<>(roleNames());
    private final JLabel messageLabel = new JLabel(" ");
    /** The account whose stored role the role picker currently shows. */
    private String lastSyncedAccount = "";
    /** True while the account picker is being rebuilt, so its events are ignored. */
    private boolean refreshing;

    /**
     * Creates the page.
     *
     * @param listAccountsController    the read that fills the page
     * @param createUserController      the Create User use case
     * @param changeUserRoleController  the Change User Role use case
     * @param deactivateUserController  the Deactivate User use case
     * @param reactivateUserController  the Reactivate User use case
     * @param viewModel                 the page state
     * @param session                   who is signed in, so the page reloads for the
     *                                  next user and empties when they leave
     */
    public ManageUsersView(ListAccountsController listAccountsController,
                           CreateUserController createUserController,
                           ChangeUserRoleController changeUserRoleController,
                           DeactivateUserController deactivateUserController,
                           ReactivateUserController reactivateUserController,
                           ManageUsersViewModel viewModel,
                           Session session) {
        this.listAccountsController = listAccountsController;
        this.createUserController = createUserController;
        this.changeUserRoleController = changeUserRoleController;
        this.deactivateUserController = deactivateUserController;
        this.reactivateUserController = reactivateUserController;
        this.viewModel = viewModel;
        this.session = session;
        this.viewModel.addPropertyChangeListener(this);
        this.session.addPropertyChangeListener(this::onSessionChanged);

        // A new account starts as the least-privileged role, as on the sign-up page.
        newRoleBox.setSelectedItem(Role.PLAYER.name());

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(28, 36, 28, 36));

        add(buildHeader());
        add(Box.createVerticalStrut(16));
        add(buildAccountList());
        add(Box.createVerticalStrut(18));
        add(buildCreateForm());
        add(Box.createVerticalStrut(18));
        add(buildActions());
        add(Box.createVerticalStrut(14));
        add(messageLabel);

        refresh();
    }

    private JPanel buildHeader() {
        final JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(Color.WHITE);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel title = new JLabel("Manage users");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel subtitle = new JLabel("Administrators only. Deactivating an account keeps "
                + "its history and refuses its next sign-in.");
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitle.setForeground(INK);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);
        return header;
    }

    private JComponent buildAccountList() {
        final JPanel panel = section("Accounts");

        accountList.setEditable(false);
        accountList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        accountList.setBackground(new Color(248, 249, 251));
        final JScrollPane scroll = new JScrollPane(accountList);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));

        final JButton refreshButton = new JButton("Refresh");
        refreshButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshButton.addActionListener(e -> onRefresh());

        panel.add(scroll);
        panel.add(Box.createVerticalStrut(8));
        panel.add(refreshButton);
        return panel;
    }

    private JPanel buildCreateForm() {
        final JPanel form = section("Create an account");
        form.add(fieldRow("Username", usernameField));
        form.add(Box.createVerticalStrut(8));
        form.add(fieldRow("Display name (optional)", displayNameField));
        form.add(Box.createVerticalStrut(8));
        form.add(fieldRow("Password", passwordField));
        form.add(Box.createVerticalStrut(8));
        form.add(fieldRow("Repeat password", repeatPasswordField));
        form.add(Box.createVerticalStrut(8));
        form.add(fieldRow("Role", newRoleBox));

        final JButton createButton = new JButton("Create account");
        createButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        createButton.addActionListener(e -> onCreate());
        form.add(Box.createVerticalStrut(12));
        form.add(createButton);
        return form;
    }

    private JPanel buildActions() {
        final JPanel actions = section("Act on an account");

        final JPanel roleRow = row();
        accountBox.setMaximumSize(new Dimension(170, 28));
        roleBox.setMaximumSize(new Dimension(130, 28));
        // Choosing another account shows that account's own role, so Change role
        // always starts from what is stored.
        accountBox.addActionListener(e -> {
            if (!refreshing) {
                syncRolePicker(viewModel.getAccounts());
            }
        });

        final JButton changeRoleButton = new JButton("Change role");
        changeRoleButton.addActionListener(e -> onChangeRole());

        roleRow.add(new JLabel("Account "));
        roleRow.add(accountBox);
        roleRow.add(Box.createHorizontalStrut(12));
        roleRow.add(new JLabel("Role "));
        roleRow.add(roleBox);
        roleRow.add(Box.createHorizontalStrut(12));
        roleRow.add(changeRoleButton);
        roleRow.add(Box.createHorizontalGlue());

        final JPanel statusRow = row();
        final JButton deactivateButton = new JButton("Deactivate");
        deactivateButton.addActionListener(e -> onDeactivate());
        final JButton reactivateButton = new JButton("Reactivate");
        reactivateButton.addActionListener(e -> onReactivate());

        final JLabel hint = new JLabel("Deactivating keeps the account's history and refuses "
                + "its next sign-in; reactivating undoes it.");
        hint.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        hint.setForeground(INK);

        statusRow.add(deactivateButton);
        statusRow.add(Box.createHorizontalStrut(8));
        statusRow.add(reactivateButton);
        statusRow.add(Box.createHorizontalStrut(16));
        statusRow.add(hint);
        statusRow.add(Box.createHorizontalGlue());

        actions.add(roleRow);
        actions.add(Box.createVerticalStrut(8));
        actions.add(statusRow);
        return actions;
    }

    private static JPanel row() {
        final JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(Color.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    private static JPanel section(String heading) {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel label = new JLabel(heading);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(label);
        panel.add(Box.createVerticalStrut(8));
        return panel;
    }

    private static JPanel fieldRow(String labelText, JComponent field) {
        final JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setBackground(Color.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        final JLabel label = new JLabel(labelText);
        label.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        label.setForeground(INK);
        label.setPreferredSize(new Dimension(170, 24));
        label.setMaximumSize(new Dimension(170, 24));

        field.setMaximumSize(new Dimension(FIELD_WIDTH, 26));
        row.add(label);
        row.add(field);
        row.add(Box.createHorizontalGlue());
        return row;
    }

    private static String[] roleNames() {
        final Role[] roles = Role.values();
        final String[] names = new String[roles.length];
        for (int i = 0; i < roles.length; i++) {
            names[i] = roles[i].name();
        }
        return names;
    }

    // ------------------------------------------------------------------
    // Behaviour
    // ------------------------------------------------------------------

    /** Presses Create account. */
    void onCreate() {
        createUserController.execute(usernameField.getText(), displayNameField.getText(),
                new String(passwordField.getPassword()),
                new String(repeatPasswordField.getPassword()),
                (String) newRoleBox.getSelectedItem());
    }

    /** Presses Change role for the selected account. */
    void onChangeRole() {
        changeUserRoleController.execute(selectedAccount(), (String) roleBox.getSelectedItem());
    }

    /** Presses Deactivate for the selected account. */
    void onDeactivate() {
        deactivateUserController.execute(selectedAccount());
    }

    /** Presses Reactivate for the selected account. */
    void onReactivate() {
        reactivateUserController.execute(selectedAccount());
    }

    /** Presses Refresh: reloads the account list. */
    void onRefresh() {
        listAccountsController.execute();
    }

    /**
     * Empties the page and asks for the list again whenever somebody signs in or
     * out.
     *
     * The page does not decide who may read accounts: the read use case checks the
     * caller and the presenter reports the outcome, so a stale list cannot be kept
     * on screen by a view that guessed wrong. Emptying first also means the next
     * user never sees the previous one's accounts, even if the read fails.
     *
     * @param evt the session change
     */
    private void onSessionChanged(PropertyChangeEvent evt) {
        if (!"session".equals(evt.getPropertyName())) {
            return;
        }
        viewModel.clear();
        listAccountsController.execute();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        refresh();
    }

    /** Redraws the list and the account picker, then shows the latest message. */
    private void refresh() {
        final List<AccountData> accounts = viewModel.getAccounts();
        accountList.setText(describeAccounts(accounts));
        refreshAccountBox(accounts);

        final String error = viewModel.getErrorMessage();
        if (!error.isEmpty()) {
            show(error, PROBLEM);
            return;
        }
        final String success = viewModel.getSuccessMessage();
        if (!success.isEmpty()) {
            show(success, OK);
            clearCreateFields();
            return;
        }
        messageLabel.setText(" ");
    }

    /**
     * Rebuilds the account picker, keeping the account that was selected so the
     * list does not jump after every action.
     *
     * @param accounts the accounts to list
     */
    private void refreshAccountBox(List<AccountData> accounts) {
        refreshing = true;
        try {
            final String previous = selectedAccount();
            accountBox.removeAllItems();
            for (AccountData account : accounts) {
                accountBox.addItem(account.getUsername());
            }
            if (!previous.isEmpty()) {
                accountBox.setSelectedItem(previous);
            }
            if (accountBox.getSelectedItem() == null && accountBox.getItemCount() > 0) {
                accountBox.setSelectedIndex(0);
            }
            syncRolePicker(accounts);
        }
        finally {
            refreshing = false;
        }
    }

    /**
     * Starts the role picker on the selected account's own role, so pressing
     * Change role cannot re-role somebody by accident. A choice the administrator
     * has made is kept while the same account stays selected, so an unrelated
     * message does not undo it.
     *
     * @param accounts the accounts currently listed
     */
    private void syncRolePicker(List<AccountData> accounts) {
        final String selected = selectedAccount();
        if (selected.equals(lastSyncedAccount)) {
            return;
        }
        lastSyncedAccount = selected;
        for (AccountData account : accounts) {
            if (account.getUsername().equals(selected)) {
                roleBox.setSelectedItem(account.getRole().name());
                return;
            }
        }
    }

    private static String describeAccounts(List<AccountData> accounts) {
        if (accounts.isEmpty()) {
            return "No accounts loaded. Sign in as an administrator.";
        }
        final StringBuilder text = new StringBuilder();
        for (AccountData account : accounts) {
            text.append(line(account)).append('\n');
        }
        return text.toString();
    }

    /** One line of the account list: who the account is, its role and its status. */
    private static String line(AccountData account) {
        return account.getUsername() + " - " + account.getDisplayName() + " - "
                + account.getRole() + (account.isActive() ? " - active" : " - deactivated");
    }

    private void clearCreateFields() {
        usernameField.setText("");
        displayNameField.setText("");
        passwordField.setText("");
        repeatPasswordField.setText("");
    }

    private void show(String message, Color colour) {
        messageLabel.setForeground(colour);
        messageLabel.setText(message);
    }

    // ------------------------------------------------------------------
    // Test seams: package-private so tests can drive the page without a mouse.
    // ------------------------------------------------------------------

    /**
     * Types the create form the way a user would.
     *
     * @param username       the new username
     * @param displayName    the display name, possibly empty
     * @param password       the chosen password
     * @param repeatPassword the same password again
     */
    void typeNewAccount(String username, String displayName, String password,
                        String repeatPassword) {
        usernameField.setText(username);
        displayNameField.setText(displayName);
        passwordField.setText(password);
        repeatPasswordField.setText(repeatPassword);
    }

    /**
     * Chooses the role for a new account.
     *
     * @param roleName the role to select
     */
    void selectNewRole(String roleName) {
        newRoleBox.setSelectedItem(roleName);
    }

    /**
     * Chooses the account the actions apply to.
     *
     * @param username the account to select
     */
    void selectAccount(String username) {
        accountBox.setSelectedItem(username);
    }

    /**
     * Chooses the role for Change role.
     *
     * @param roleName the role to select
     */
    void selectRole(String roleName) {
        roleBox.setSelectedItem(roleName);
    }

    /**
     * Reads back the message line.
     *
     * @return the message currently shown
     */
    String status() {
        return messageLabel.getText();
    }

    /**
     * Reads back the account list as one string.
     *
     * @return the lines currently listed
     */
    String listedAccounts() {
        return accountList.getText();
    }

    /**
     * Reads back the selected account.
     *
     * @return the username in the picker, or an empty string
     */
    String selectedAccount() {
        final Object selected = accountBox.getSelectedItem();
        return selected == null ? "" : selected.toString();
    }

    /** Presses Create account the way a test would. */
    void clickCreate() {
        onCreate();
    }

    /** Presses Change role the way a test would. */
    void clickChangeRole() {
        onChangeRole();
    }

    /** Presses Deactivate the way a test would. */
    void clickDeactivate() {
        onDeactivate();
    }

    /** Presses Refresh the way a test would. */
    void clickRefresh() {
        onRefresh();
    }

    /** Presses Reactivate the way a test would. */
    void clickReactivate() {
        onReactivate();
    }
}
