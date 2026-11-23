package use_case.rsvp;
public interface RsvpOutputBoundary {
 void prepareSuccessView(RsvpOutputData data);
 void prepareFailView(String message);
}
