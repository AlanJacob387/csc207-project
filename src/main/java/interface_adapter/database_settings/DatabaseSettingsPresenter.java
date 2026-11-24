package interface_adapter.database_settings;

import use_case.database_settings.DatabaseSettingsOutputBoundary;
import use_case.database_settings.DatabaseSettingsOutputData;

/**
 * Presenter for the Database Settings use case.
 *
 * It puts the configured values and the outcome into the view model; the page
 * only reads that and draws it.
 */
public class DatabaseSettingsPresenter implements DatabaseSettingsOutputBoundary {

    private final DatabaseSettingsViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the page state
     */
    public DatabaseSettingsPresenter(DatabaseSettingsViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareView(String currentUrl, String settingsFile) {
        viewModel.showSettings(currentUrl, settingsFile);
    }

    @Override
    public void prepareResult(DatabaseSettingsOutputData outputData) {
        viewModel.showResult(outputData.isOk(), outputData.getMessage());
    }
}
