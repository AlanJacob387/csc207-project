package interface_adapter.view_event_weather;

import use_case.view_event_weather.ViewEventWeatherOutputBoundary;
import use_case.view_event_weather.ViewEventWeatherOutputData;

/** Applies a completed, still-current request on the Swing event thread. */
public class ViewEventWeatherPresenter implements ViewEventWeatherOutputBoundary {
    private final ViewEventWeatherViewModel model;
    /** Creates a presenter for a completed authorized weather request. */
    public ViewEventWeatherPresenter(ViewEventWeatherViewModel model) {
        this.model = model;
    }
    @Override
    public void prepareSuccessView(ViewEventWeatherOutputData data) {
        model.showData(data);
    }
    @Override
    public void prepareFailView(String message) {
        model.showError(message);
    }
}
