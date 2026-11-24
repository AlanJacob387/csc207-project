package use_case.flag_rain_risk;

public interface FlagRainRiskOutputBoundary {
    void prepareSuccessView(FlagRainRiskOutputData outputData);
    void prepareFailView(String message);
}
