package interface_adapter.signup;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import entity.Role;

/**
 * View model for the Sign Up view.
 * The view observes this and shows whatever error is current, and takes the roles
 * it may offer from here.
 *
 * The choices come from {@link Role#selfAssignableNames()}, so the page holds no
 * opinion of its own about which roles may sign up: it displays what the model
 * carries.
 */
public class SignupViewModel {

    private final PropertyChangeSupport support = new PropertyChangeSupport(this);

    private final List<String> roleChoices;

    private String errorMessage = "";

    /** Creates the view model with the roles the rule allows at sign-up. */
    public SignupViewModel() {
        this(Role.selfAssignableNames());
    }

    /**
     * Creates the view model with an explicit set of roles, so a test can check
     * that the page shows what it is given.
     *
     * @param roleChoices the role names to offer
     */
    public SignupViewModel(List<String> roleChoices) {
        this.roleChoices = List.copyOf(roleChoices);
    }

    /**
     * The roles the sign-up page may offer.
     *
     * @return the role names, in the order they should be shown
     */
    public List<String> getRoleChoices() {
        return roleChoices;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Shows an error.
     *
     * @param errorMessage the message to display
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        support.firePropertyChange("state", null, this);
    }

    /** Clears the error, for example when the page is opened again. */
    public void clear() {
        this.errorMessage = "";
        support.firePropertyChange("state", null, this);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }
}
