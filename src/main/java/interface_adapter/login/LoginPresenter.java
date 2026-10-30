package interface_adapter.login;

import interface_adapter.Session;
import interface_adapter.ViewManagerModel;
import use_case.login.LoginOutputBoundary;
import use_case.login.LoginOutputData;

/**
 * Presenter for the Log In use case.
 *
 * On success it fills the session, clears the login page and asks the view
 * manager to show the signed-in shell; on failure it puts the error on the
 * login view model.
 */
public class LoginPresenter implements LoginOutputBoundary {

    private final ViewManagerModel viewManagerModel;
    private final LoginViewModel loginViewModel;
    private final Session session;

    /**
     * Creates the presenter.
     *
     * @param viewManagerModel controls which page is visible
     * @param loginViewModel   the login page's state
     * @param session          who is signed in
     */
    public LoginPresenter(ViewManagerModel viewManagerModel,
                          LoginViewModel loginViewModel,
                          Session session) {
        this.viewManagerModel = viewManagerModel;
        this.loginViewModel = loginViewModel;
        this.session = session;
    }

    @Override
    public void prepareSuccessView(LoginOutputData outputData) {
        session.logIn(outputData.getUsername(), outputData.getDisplayName(),
                outputData.getRole());
        loginViewModel.clear();
        viewManagerModel.setState(ViewManagerModel.APP);
        viewManagerModel.firePropertyChanged();
    }

    @Override
    public void prepareFailView(String errorMessage) {
        loginViewModel.setErrorMessage(errorMessage);
    }
}
