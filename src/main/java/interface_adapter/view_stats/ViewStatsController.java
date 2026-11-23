package interface_adapter.view_stats;
import use_case.view_stats.ViewStatsInputBoundary;
import use_case.view_stats.ViewStatsInputData;
public final class ViewStatsController {
 private final ViewStatsInputBoundary interactor;
 public ViewStatsController(ViewStatsInputBoundary interactor) { this.interactor = interactor; }
 public void execute() { interactor.execute(new ViewStatsInputData()); }
}
