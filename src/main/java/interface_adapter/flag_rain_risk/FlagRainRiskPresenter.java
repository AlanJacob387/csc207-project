package interface_adapter.flag_rain_risk;

import use_case.flag_rain_risk.FlagRainRiskOutputBoundary;
import use_case.flag_rain_risk.FlagRainRiskOutputData;

public class FlagRainRiskPresenter implements FlagRainRiskOutputBoundary {
    private final FlagRainRiskViewModel model;
    /** Creates a presenter for a completed authorized weather request. */
    public FlagRainRiskPresenter(FlagRainRiskViewModel model) {
        this.model = model;
    }
    @Override
    public void prepareSuccessView(FlagRainRiskOutputData data) {
        model.showData(data);
    }
    @Override
    public void prepareFailView(String message) {
        model.showError(message);
    }
}
