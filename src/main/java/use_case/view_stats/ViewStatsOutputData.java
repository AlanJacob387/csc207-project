package use_case.view_stats;
/** Attendance count summary; excused absences are excluded from the rate denominator. */
public final class ViewStatsOutputData {
 private final int present;
 private final int absent;
 private final int excused;
 public ViewStatsOutputData(int present, int absent, int excused) {
  this.present = present; this.absent = absent; this.excused = excused;
 }
 public int getPresent() { return present; }
 public int getAbsent() { return absent; }
 public int getExcused() { return excused; }
 public double getAttendanceRate() {
  return present + absent == 0 ? 0.0 : (double) present / (present + absent);
 }
}
