package use_case.view_event_weather;

public interface ViewEventWeatherOutputBoundary {
    void prepareSuccessView(ViewEventWeatherOutputData outputData);
    void prepareFailView(String message);
}
