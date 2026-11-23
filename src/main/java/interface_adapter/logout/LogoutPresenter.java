package interface_adapter.logout;

import interface_adapter.ViewManagerModel;
import interface_adapter.login.LoginViewModel;
import use_case.logout.LogoutOutputBoundary;
import use_case.logout.LogoutOutputData;

/**
 * Presenter for the Log Out use case.
 *
 * Clears anything left over on the login page and asks the view manager to show
 * it, so the use case decides the navigation rather than the view.
 */
public class LogoutPresenter implements LogoutOutputBoundary {

    private final ViewManagerModel viewManagerModel;
    private final LoginViewModel loginViewModel;

    /**
     * Creates the presenter.
     *
     * @param viewManagerModel controls which page is visible
     * @param loginViewModel   the login page state
     */
    public LogoutPresenter(ViewManagerModel viewManagerModel, LoginViewModel loginViewModel) {
        this.viewManagerModel = viewManagerModel;
        this.loginViewModel = loginViewModel;
    }

    @Override
    public void prepareSuccessView(LogoutOutputData outputData) {
        loginViewModel.clear();
        navigateToLogin();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        loginViewModel.setErrorMessage(errorMessage);
        navigateToLogin();
    }

    /** Asks the view manager to show the login page. */
    private void navigateToLogin() {
        viewManagerModel.setState(ViewManagerModel.LOGIN);
        viewManagerModel.firePropertyChanged();
    }
}