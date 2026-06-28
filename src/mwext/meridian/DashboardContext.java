package mwext.meridian;

import com.motivewave.platform.sdk.common.DataSeries;

import java.awt.Color;

final class DashboardContext {
  final int closedIndex;
  final int stateIndex;
  final int bars;
  final BacktestStats stats;
  final OptimizerResult optimizerResult;
  final String optimizerStatusValue;
  final String optimizerStatusExtra;
  final int optimizerStatusKind;
  final String signal;
  final Color signalColor;
  final double winRate;
  final double profitFactor;
  final double netProfitFactor;
  final double recoveryFactor;
  final boolean recommendationAvailable;
  final boolean showOptimizerApply;
  final String coreFilters;
  final String strategyFilters;

  private DashboardContext(int closedIndex, int stateIndex, int bars, BacktestStats stats,
                           OptimizerResult optimizerResult, String optimizerStatusValue,
                           String optimizerStatusExtra, int optimizerStatusKind, String signal,
                           Color signalColor, double winRate, double profitFactor,
                           double netProfitFactor, double recoveryFactor,
                           boolean recommendationAvailable, boolean showOptimizerApply,
                           String coreFilters, String strategyFilters) {
    this.closedIndex = closedIndex;
    this.stateIndex = stateIndex;
    this.bars = bars;
    this.stats = stats;
    this.optimizerResult = optimizerResult;
    this.optimizerStatusValue = optimizerStatusValue;
    this.optimizerStatusExtra = optimizerStatusExtra;
    this.optimizerStatusKind = optimizerStatusKind;
    this.signal = signal;
    this.signalColor = signalColor;
    this.winRate = winRate;
    this.profitFactor = profitFactor;
    this.netProfitFactor = netProfitFactor;
    this.recoveryFactor = recoveryFactor;
    this.recommendationAvailable = recommendationAvailable;
    this.showOptimizerApply = showOptimizerApply;
    this.coreFilters = coreFilters;
    this.strategyFilters = strategyFilters;
  }

  static DashboardContext create(DataSeries s, SettingsView cfg, int signalIndex, int marketIndex,
                                 boolean[] longSignals, boolean[] shortSignals, Projection projection,
                                 int structTrend, double lastSwingHigh, double lastSwingLow,
                                 boolean swingHighBroken, boolean swingLowBroken,
                                 boolean[] forgeLong, boolean[] forgeShort, HtfBias htfBias,
                                 double[] atrRisk, double[] rsi, Stoch stoch, Sar sar,
                                 Tilson tilson, Smi smi, MeridianOptimizer optimizer) {
    if (s == null || cfg == null || signalIndex < 0 || signalIndex >= s.size()) return null;

    int closedIndex = Math.max(0, Math.min(signalIndex, s.size() - 1));
    int stateIndex = Math.max(0, Math.min(marketIndex, s.size() - 1));
    BacktestStats stats = MeridianBacktest.runBacktest(s, cfg, closedIndex, longSignals, shortSignals, atrRisk, cfg.dashboardLookback);
    OptimizerResult opt = cfg.showOptimizer && optimizer != null ? optimizer.currentResult(s, cfg) : null;
    int bars = Math.min(cfg.dashboardLookback, closedIndex + 1);

    boolean longSignalNow = stateIndex == closedIndex && longSignals != null && closedIndex < longSignals.length && longSignals[closedIndex];
    boolean shortSignalNow = stateIndex == closedIndex && shortSignals != null && closedIndex < shortSignals.length && shortSignals[closedIndex];
    boolean forgeLongNow = forgeLong != null && stateIndex < forgeLong.length && forgeLong[stateIndex];
    boolean forgeShortNow = forgeShort != null && stateIndex < forgeShort.length && forgeShort[stateIndex];
    boolean htfLongNow = !cfg.useHtf || (htfBias != null && stateIndex < htfBias.bull.length && htfBias.bull[stateIndex]);
    boolean htfShortNow = !cfg.useHtf || (htfBias != null && stateIndex < htfBias.bear.length && htfBias.bear[stateIndex]);
    double breakHighSrc = cfg.breakOnWick ? s.getHigh(stateIndex) : s.getClose(stateIndex);
    double breakLowSrc = cfg.breakOnWick ? s.getLow(stateIndex) : s.getClose(stateIndex);
    SignalSourceOption signalSource = SignalSourceOption.from(cfg.signalSource);
    String signal = DashboardSupport.stateLabel(longSignalNow, shortSignalNow, projection, structTrend,
      breakHighSrc, breakLowSrc, lastSwingHigh, lastSwingLow, swingHighBroken, swingLowBroken,
      forgeLongNow, forgeShortNow, htfLongNow, htfShortNow,
      signalSource.usesStructure(), signalSource.usesForge());
    Color signalColor = DashboardSupport.stateColor(signal, cfg);

    double winRate = stats.trades == 0 ? 0.0 : 100.0 * stats.wins / stats.trades;
    double profitFactor = MeridianBacktest.profitFactor(stats);
    double netProfitFactor = MeridianBacktest.netProfitFactor(stats);
    double recoveryFactor = MeridianBacktest.recoveryFactor(stats);
    boolean recommendationAvailable = opt != null && opt.valid && opt.cfg != null && !DashboardSupport.sameTuning(cfg, opt.cfg);
    String optimizerStatusValue = optimizer == null ? "" : optimizer.statusValue();
    String optimizerStatusExtra = optimizer == null ? "" : optimizer.statusExtra();
    int optimizerStatusKind = optimizer == null ? 0 : optimizer.statusKind();
    boolean manualOptimizerWaiting = cfg.showOptimizer && ("READY".equals(optimizerStatusValue) || "STALE".equals(optimizerStatusValue));
    boolean showOptimizerApply = recommendationAvailable || manualOptimizerWaiting;
    String coreFilters = DashboardSupport.coreFilterSnapshot(cfg, stateIndex, s.getClose(stateIndex), rsi, stoch, sar);
    String strategyFilters = DashboardSupport.strategyFilterSnapshot(cfg, stateIndex, tilson, smi);

    return new DashboardContext(closedIndex, stateIndex, bars, stats, opt, optimizerStatusValue,
      optimizerStatusExtra, optimizerStatusKind, signal, signalColor, winRate, profitFactor,
      netProfitFactor, recoveryFactor, recommendationAvailable, showOptimizerApply,
      coreFilters, strategyFilters);
  }
}
