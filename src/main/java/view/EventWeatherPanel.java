package view;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import interface_adapter.Session;
import interface_adapter.view_event_weather.ViewEventWeatherController;
import interface_adapter.view_event_weather.ViewEventWeatherViewModel;
import use_case.view_event_weather.ViewEventWeatherOutputData;
import use_case.view_event_weather.WeatherForecast;

/** Explicit weather lookup with a city/coordinates correction for ambiguous venue names. */
public class EventWeatherPanel extends JPanel {
    private final ViewEventWeatherController controller;
    private final ViewEventWeatherViewModel model;
    private final Session session;
    private final JTextField location = new JTextField(25);
    private final JButton check = new JButton("Check weather");
    private final JTextArea result = new JTextArea(5, 40);
    private String eventId;

    /** Connects an accessible on-demand forecast panel to the shared session. */
    public EventWeatherPanel(ViewEventWeatherController controller, ViewEventWeatherViewModel model, Session session) {
        this.controller = controller;
        this.model = model;
        this.session = session;
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Event weather"));
        final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JLabel label = new JLabel("City or latitude,longitude");
        label.setLabelFor(location);
        location.setName("weatherLocation");
        location.getAccessibleContext().setAccessibleName("Weather location, city or latitude and longitude");
        controls.add(label);
        controls.add(location);
        controls.add(check);
        add(controls, BorderLayout.NORTH);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.getAccessibleContext().setAccessibleName("Event forecast and weather status");
        result.setName("eventWeatherResult");
        add(new JScrollPane(result), BorderLayout.CENTER);
        final JLabel hint = new JLabel("Examples: Toronto, Ontario or 43.66,-79.39. Weather: Open-Meteo (CC BY 4.0).");
        add(hint, BorderLayout.SOUTH);
        check.addActionListener(event -> controller.execute(eventId, location.getText()));
        model.addPropertyChangeListener(event -> render());
        session.addPropertyChangeListener(event -> clear());
        render();
    }

    /** Loads only authorization and event metadata. Network access waits for Check weather. */
    public void showEvent(String value) {
        eventId = value;
        location.setText("");
        controller.clear();
        if (value != null && !value.isBlank() && session.isLoggedIn()) {
            controller.describe(value);
        }
    }

    /** Cancels loading and removes weather belonging to the previous context. */
    public void clear() {
        eventId = null;
        location.setText("");
        controller.clear();
    }

    private void render() {
        final ViewEventWeatherOutputData data = model.getData();
        final boolean canCheck = session.isLoggedIn() && eventId != null && !model.isLoading()
                && (data == null || data.canCheck());
        check.setEnabled(canCheck);
        location.setEnabled(canCheck);
        if (data != null && location.getText().isBlank()) {
            location.setText(data.location());
        }
        final StringBuilder text = new StringBuilder(model.getMessage());
        if (data != null && data.forecast() != null) {
            final WeatherForecast forecast = data.forecast();
            text.append("\n").append(forecast.date()).append(" — ").append(forecast.location())
                    .append("\nHigh: ").append(forecast.maxTemperatureCelsius()).append(" °C; rain chance: ")
                    .append(forecast.precipitationProbabilityPercent()).append("%")
                    .append("\n").append(forecast.isRainRisk() ? "Rain risk (60% or higher)." : "Below the 60% rain-risk threshold.")
                    .append("\n").append(forecast.stale() ? "Stale forecast saved at " : "Fetched at ")
                    .append(forecast.fetchedAt());
        }
        result.setText(text.toString());
        result.setCaretPosition(0);
    }
}
