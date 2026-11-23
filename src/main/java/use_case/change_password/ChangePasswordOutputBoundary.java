package use_case.change_password;

/**
 * Output boundary for the Change Password use case.
 */
public interface ChangePasswordOutputBoundary {

    /**
     * Called when the password was changed.
     *
     * @param outputData the result to show
     */
    void prepareSuccessView(ChangePasswordOutputData outputData);

    /**
     * Called when the password could not be changed.
     *
     * @param errorMessage a message to show on the account page
     */
    void prepareFailView(String errorMessage);
}