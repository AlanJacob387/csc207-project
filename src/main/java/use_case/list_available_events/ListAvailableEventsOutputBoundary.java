package use_case.list_available_events;
import java.util.List;
public interface ListAvailableEventsOutputBoundary {
 void prepareSuccessView(List<AvailableEvent> events);
 void prepareFailView(String message);
}
