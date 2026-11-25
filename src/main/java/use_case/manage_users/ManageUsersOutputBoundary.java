package use_case.manage_users;

/**
 * Output boundary for the Manage Users use cases.
 *
 * The three actions share one boundary because they share one page: whichever
 * action runs, the administrator sees either the refreshed account list with a
 * message, or a message explaining why nothing changed.
 */
public interface ManageUsersOutputBoundary {

    /**
     * Shows a successful action and the refreshed accounts.
     *
     * @param outputData the message and the current accounts
     */
    void prepareSuccessView(ManageUsersOutputData outputData);

    /**
     * Explains why the action was refused.
     *
     * @param errorMessage the message to display
     */
    void prepareFailView(String errorMessage);
}
