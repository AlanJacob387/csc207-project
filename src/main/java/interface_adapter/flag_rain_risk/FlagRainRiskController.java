package interface_adapter.flag_rain_risk;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import use_case.current_user.CurrentUserInterface;
import use_case.flag_rain_risk.FlagRainRiskDataAccessInterface;
import use_case.flag_rain_risk.FlagRainRiskInputData;
import use_case.flag_rain_risk.FlagRainRiskInteractor;
import use_case.flag_rain_risk.FlagRainRiskOutputBoundary;
import use_case.flag_rain_risk.FlagRainRiskOutputData;
import use_case.view_event_weather.EventWeatherGateway;

/** Checks the visible upcoming events in a cancellable worker after an explicit user action. */
public class FlagRainRiskController {
    private final FlagRainRiskDataAccessInterface data;
    private final EventWeatherGateway gateway;
    private final FlagRainRiskViewModel model;
    private final CurrentUserInterface session;
    private long revision;
    private SwingWorker<Result, Void> worker;

    /** Connects stored membership, forecast access, and the current session. */
    public FlagRainRiskController(FlagRainRiskDataAccessInterface data, EventWeatherGateway gateway,
                                  FlagRainRiskViewModel model, CurrentUserInterface session) {
        this.data = data;
        this.gateway = gateway;
        this.model = model;
        this.session = session;
    }

    /** Checks all accessible upcoming events, or one explicitly selected team. */
    public void execute(String teamId) {
        final long requestRevision = ++revision;
        if (worker != null) {
            worker.cancel(true);
        }
        final String username = session.getCurrentUsername();
        model.showLoading();
        worker = new SwingWorker<>() {
            @Override
            protected Result doInBackground() {
                final Result result = new Result();
                new FlagRainRiskInteractor(data, result, gateway).execute(new FlagRainRiskInputData(username, teamId));
                return result;
            }
            @Override
            protected void done() {
                if (requestRevision != revision || !Objects.equals(username, session.getCurrentUsername())) {
                    return;
                }
                final FlagRainRiskPresenter presenter = new FlagRainRiskPresenter(model);
                try {
                    final Result result = get();
                    if (result.data != null) {
                        presenter.prepareSuccessView(result.data);
                    }
                    else {
                        presenter.prepareFailView(result.error == null ? "Rain risk unavailable." : result.error);
                    }
                }
                catch (CancellationException ex) {
                    /* A new request or session now owns this state. */
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException ex) {
                    presenter.prepareFailView("Rain risk unavailable. Please try again.");
                }
            }
        };
        worker.execute();
    }

    /** Cancels outstanding work and removes the previous request state. */
    public void clear() {
        revision++;
        if (worker != null) {
            worker.cancel(true);
        }
        model.clear();
    }

    private static final class Result implements FlagRainRiskOutputBoundary {
        private FlagRainRiskOutputData data;
        private String error;
        @Override
        public void prepareSuccessView(FlagRainRiskOutputData value) {
            data = value;
        }

        @Override
        public void prepareFailView(String value) {
            error = value;
        }
    }
}
