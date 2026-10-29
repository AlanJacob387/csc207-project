package interface_adapter.signup;

import interface_adapter.ViewManagerModel;
import interface_adapter.login.LoginViewModel;
import use_case.signup.SignupOutputBoundary;
import use_case.signup.SignupOutputData;

/**
 * Presenter for the Sign Up use case.
 *
 * On success it puts a friendly message on the login page and sends the user
 * back there to log in; on failure it shows the error on the sign-up page.
 */
public class SignupPresenter implements SignupOutputBoundary {

    private final ViewManagerModel viewManagerModel;
    private final SignupViewModel signupViewModel;
    private final LoginViewModel loginViewModel;

    /**
     * Creates the presenter.
     *
     * @param viewManagerModel controls which page is visible
     * @param signupViewModel  the sign-up page's state
     * @param loginViewModel   the login page's state, for the success message
     */
    public SignupPresenter(ViewManagerModel viewManagerModel,
                           SignupViewModel signupViewModel,
                           LoginViewModel loginViewModel) {
        this.viewManagerModel = viewManagerModel;
        this.signupViewModel = signupViewModel;
        this.loginViewModel = loginViewModel;
    }

    @Override
    public void prepareSuccessView(SignupOutputData outputData) {
        signupViewModel.clear();
        loginViewModel.setSuccessMessage(outputData.getMessage());
        viewManagerModel.setState(ViewManagerModel.LOGIN);
        viewManagerModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        signupViewModel.setErrorMessage(errorMessage);
    }
}
