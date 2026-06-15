package mwext.meridian;

final class OptimizerResult {
  boolean valid;
  int candidates;
  int bars;
  int computedAtBar = -1;
  long computeMillis;
  double score;
  String objective;
  String params;
  String note;
  BacktestStats stats;
  SettingsView cfg;
}

final class OptimizerAccumulator {
  int candidates;
  OptimizerResult best;
  OptimizerResult fallback;
}
