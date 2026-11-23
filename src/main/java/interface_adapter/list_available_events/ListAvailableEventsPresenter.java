package interface_adapter.list_available_events;
import java.util.List;
import use_case.list_available_events.*;
public final class ListAvailableEventsPresenter implements ListAvailableEventsOutputBoundary {
 private final ListAvailableEventsViewModel viewModel;
 public ListAvailableEventsPresenter(ListAvailableEventsViewModel viewModel) {
  this.viewModel = viewModel;
 }
 @Override public void prepareSuccessView(List<AvailableEvent> events) { viewModel.setEvents(events); }
 @Override public void prepareFailView(String message) { viewModel.setError(message); }
}
