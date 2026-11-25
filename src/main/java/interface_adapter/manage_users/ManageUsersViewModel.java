package interface_adapter.manage_users;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import use_case.manage_users.ManageUsersOutputData;
import use_case.manage_users.ManageUsersOutputData.AccountData;

/**
 * State of the administrator page: the account list and the last message.
 *
 * A failed action keeps the list on screen, because the administrator needs to see
 * what they were acting on while reading the explanation.
 */
public class ManageUsersViewModel {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private List<AccountData> accounts = List.of();
    private String errorMessage = "";
    private String successMessage = "";

    public List<AccountData> getAccounts() {
        return accounts;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    /**
     * Shows a refreshed account list together with the message that explains the
     * change.
     *
     * @param outputData the message and the current accounts
     */
    public void showAccounts(ManageUsersOutputData outputData) {
        this.accounts = outputData.getAccounts();
        this.successMessage = outputData.getMessage();
        this.errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /**
     * Reports a refusal while keeping the list that is already on screen.
     *
     * @param message the message to display
     */
    public void showError(String message) {
        this.errorMessage = message;
        this.successMessage = "";
        support.firePropertyChange("state", null, this);
    }

    /** Clears the list and both messages, for example when the user changes. */
    public void clear() {
        this.accounts = List.of();
        this.errorMessage = "";
        this.successMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
