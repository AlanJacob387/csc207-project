package interface_adapter.view_event_weather;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;
import use_case.current_user.CurrentUserInterface;
import use_case.view_event_weather.EventWeatherGateway;
import use_case.view_event_weather.ViewEventWeatherDataAccessInterface;
import use_case.view_event_weather.ViewEventWeatherInputData;
import use_case.view_event_weather.ViewEventWeatherInteractor;
import use_case.view_event_weather.ViewEventWeatherOutputBoundary;
import use_case.view_event_weather.ViewEventWeatherOutputData;

/** Runs network work away from Swing and drops completed requests after an identity/event switch. */
public class ViewEventWeatherController {
    private final ViewEventWeatherDataAccessInterface data;
    private final EventWeatherGateway gateway;
    private final ViewEventWeatherViewModel model;
    private final CurrentUserInterface session;
    private long revision;
    private SwingWorker<Result, Void> worker;

    /** Connects stored membership, forecast access, and the current session. */
    public ViewEventWeatherController(ViewEventWeatherDataAccessInterface data, EventWeatherGateway gateway,
                                     ViewEventWeatherViewModel model, CurrentUserInterface session) {
        this.data = data;
        this.gateway = gateway;
        this.model = model;
        this.session = session;
    }

    /** Authorizes and describes the event without making any network call. */
    public void describe(String eventId) {
        start(eventId, null, false);
    }

    /** An explicit user action checks a city, region, or coordinate pair. */
    public void execute(String eventId, String query) {
        start(eventId, query, true);
    }

    /** Cancels outstanding work and removes the previous request state. */
    public void clear() {
        revision++;
        if (worker != null) {
            worker.cancel(true);
        }
        model.clear();
    }

    private void start(String eventId, String query, boolean fetch) {
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
                new ViewEventWeatherInteractor(data, result, gateway).execute(
                        new ViewEventWeatherInputData(username, eventId, query, fetch));
                return result;
            }
            @Override
            protected void done() {
                if (requestRevision != revision || !Objects.equals(username, session.getCurrentUsername())) {
                    return;
                }
                final ViewEventWeatherPresenter presenter = new ViewEventWeatherPresenter(model);
                try {
                    final Result result = get();
                    if (result.data != null) {
                        presenter.prepareSuccessView(result.data);
                    }
                    else {
                        presenter.prepareFailView(result.error == null ? "Weather unavailable." : result.error);
                    }
                }
                catch (CancellationException ex) {
                    /* A newer request owns the page. */
                }
                catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException ex) {
                    presenter.prepareFailView("Weather unavailable. Please try again.");
                }
            }
        };
        worker.execute();
    }

    private static final class Result implements ViewEventWeatherOutputBoundary {
        private ViewEventWeatherOutputData data;
        private String error;
        @Override
        public void prepareSuccessView(ViewEventWeatherOutputData value) {
            data = value;
        }

        @Override
        public void prepareFailView(String value) {
            error = value;
        }
    }
}
