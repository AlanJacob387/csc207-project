package use_case.flag_rain_risk;

import java.time.LocalDateTime;
import java.util.List;

/** Unavailable values remain distinct from a measured low precipitation probability. */
public record FlagRainRiskOutputData(List<EventRisk> events) {
    public FlagRainRiskOutputData {
        events = List.copyOf(events);
    }

    public record EventRisk(String eventId, String teamId, LocalDateTime dateTime, String location,
                            String status, Integer precipitationProbabilityPercent,
                            boolean rainRisk, boolean stale) { }
}
