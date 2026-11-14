package interface_adapter.export_schedule;

import use_case.export_schedule.ExportScheduleOutputBoundary;
import use_case.export_schedule.ExportScheduleOutputData;

/**
 * Presenter for the Export Schedule use case.
 */
public class ExportSchedulePresenter implements ExportScheduleOutputBoundary {
    private final ExportScheduleViewModel viewModel;

    /**
     * Creates the presenter.
     *
     * @param viewModel the export result's view model
     */
    public ExportSchedulePresenter(ExportScheduleViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public void prepareSuccessView(ExportScheduleOutputData outputData) {
        final int count = outputData.getEventCount();
        viewModel.setResult(ExportScheduleViewModel.Status.SUCCESS,
                "Exported " + count + (count == 1 ? " event" : " events")
                        + " to " + outputData.getTargetPath()
                        + "\nImport this file into Google Calendar, Outlook, or Apple Calendar.");
    }

    @Override
    public void prepareNothingToExportView(String message) {
        viewModel.setResult(ExportScheduleViewModel.Status.INFO, message);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.setResult(ExportScheduleViewModel.Status.ERROR, errorMessage);
    }
}
