package interface_adapter.list_available_events;
import use_case.list_available_events.ListAvailableEventsInputBoundary;
public final class ListAvailableEventsController {
 private final ListAvailableEventsInputBoundary interactor;
 public ListAvailableEventsController(ListAvailableEventsInputBoundary interactor) {
  this.interactor = interactor;
 }
 public void execute() { interactor.execute(); }
}
