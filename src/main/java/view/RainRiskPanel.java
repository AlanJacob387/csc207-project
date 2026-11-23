package view;

import java.awt.BorderLayout;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import interface_adapter.Session;
import interface_adapter.flag_rain_risk.FlagRainRiskController;
import interface_adapter.flag_rain_risk.FlagRainRiskViewModel;
import use_case.flag_rain_risk.FlagRainRiskOutputData;

/** Checks upcoming authorized events on demand and publishes measured flags to schedule views. */
public class RainRiskPanel extends JPanel {
    private final FlagRainRiskController controller;
    private final FlagRainRiskViewModel model;
    private final Session session;
    private final JButton check = new JButton("Check rain risk");
    private final JTextArea result = new JTextArea(8, 45);
    private Consumer<Map<String, String>> onFlags = flags -> {
    };
    private String teamId;

    /** Connects an accessible on-demand forecast panel to the shared session. */
    public RainRiskPanel(FlagRainRiskController controller, FlagRainRiskViewModel model, Session session) {
        this.controller = controller;
        this.model = model;
        this.session = session;
        setLayout(new BorderLayout(0, 6));
        setBorder(BorderFactory.createTitledBorder("Upcoming rain risk"));
        add(check, BorderLayout.NORTH);
        result.setEditable(false);
        result.setLineWrap(true);
        result.setWrapStyleWord(true);
        result.setName("rainRiskResult");
        result.getAccessibleContext().setAccessibleName("Upcoming event rain risk and unavailable forecasts");
        add(new JScrollPane(result), BorderLayout.CENTER);
        add(new JLabel("Forecasts: Open-Meteo (CC BY 4.0). Cancelled events are excluded."), BorderLayout.SOUTH);
        check.addActionListener(event -> refresh());
        model.addPropertyChangeListener(event -> render());
        session.addPropertyChangeListener(event -> {
            teamId = null;
            controller.clear();
        });
        render();
    }

    /** Starts the explicit upcoming-event forecast check. */
    public void refresh() {
        if (session.isLoggedIn()) {
            controller.execute(teamId);
        }
    }

    /** Changes the scope without starting network calls. A null team includes all accessible teams. */
    public void setTeamId(String value) {
        teamId = value;
        controller.clear();
    }

    /** Cancels loading and removes weather belonging to the previous context. */
    public void clear() {
        controller.clear();
    }

    /** Publishes the measured risk labels used by the calendar and event list. */
    public void setOnFlags(Consumer<Map<String, String>> callback) {
        if (callback == null) {
            onFlags = flags -> { };
        }
        else {
            onFlags = callback;
        }
        onFlags.accept(model.getFlags());
    }

    private void render() {
        check.setEnabled(session.isLoggedIn() && !model.isLoading());
        final StringBuilder text = new StringBuilder(model.getMessage());
        final FlagRainRiskOutputData data = model.getData();
        if (data != null) {
            for (FlagRainRiskOutputData.EventRisk event : data.events()) {
                text.append("\n\n").append(event.dateTime()).append(" — ").append(event.location())
                        .append("\n").append(event.status());
            }
        }
        result.setText(text.toString());
        result.setCaretPosition(0);
        onFlags.accept(model.getFlags());
    }
}
