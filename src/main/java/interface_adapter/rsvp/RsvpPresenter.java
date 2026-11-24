package interface_adapter.rsvp;
import java.util.List;
import use_case.rsvp.*;
public final class RsvpPresenter implements RsvpOutputBoundary {
 private final RsvpViewModel vm;
 public RsvpPresenter(RsvpViewModel vm) { this.vm = vm; }
 @Override public void prepareSuccessView(RsvpOutputData data) {
  vm.setState(data.getMessage(), data.getResponses());
 }
 @Override public void prepareFailView(String message) { vm.setState(message, List.of()); }
}
