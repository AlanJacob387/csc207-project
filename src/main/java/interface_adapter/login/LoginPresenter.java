package interface_adapter.login;

import interface_adapter.ViewManagerModel;
import use_case.login.LoginOutputBoundary;
import use_case.login.LoginOutputData;

/**
 * Presenter for the Log In use case.
 *
 * On success it clears the login page and asks the view manager to show the
 * signed-in shell. It no longer touches the session: the interactor recorded
 * the user before calling it.
 */
public class LoginPresenter implements LoginOutputBoundary {

    private final ViewManagerModel viewManagerModel;
    private final LoginViewModel loginViewModel;

    /**
     * Creates the presenter.
     *
     * @param viewManagerModel controls which page is visible
     * @param loginViewModel   the login page state
     */
    public LoginPresenter(ViewManagerModel viewManagerModel, LoginViewModel loginViewModel) {
        this.viewManagerModel = viewManagerModel;
        this.loginViewModel = loginViewModel;
    }

    @Override
    public void prepareSuccessView(LoginOutputData outputData) {
        loginViewModel.clear();
        viewManagerModel.setState(ViewManagerModel.APP);
        viewManagerModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        loginViewModel.setErrorMessage(errorMessage);
    }
}