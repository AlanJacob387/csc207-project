package interface_adapter.view_stats;
import use_case.view_stats.*;
public final class ViewStatsPresenter implements ViewStatsOutputBoundary {
 private final ViewStatsViewModel vm;
 public ViewStatsPresenter(ViewStatsViewModel vm) { this.vm = vm; }
 @Override public void prepareSuccessView(ViewStatsOutputData data) { vm.setData(data); }
 @Override public void prepareFailView(String message) { vm.setError(message); }
}
