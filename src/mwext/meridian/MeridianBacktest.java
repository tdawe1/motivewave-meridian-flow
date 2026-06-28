package mwext.meridian;

import com.motivewave.platform.sdk.common.DataSeries;

final class MeridianBacktest {
  private MeridianBacktest() {}

  static BacktestStats runBacktest(DataSeries s, SettingsView cfg, int signalIndex, boolean[] longSignals, boolean[] shortSignals,
                                   double[] atrRisk, int lookbackBars) {
    BacktestStats stats = new BacktestStats();
    int start = Math.max(0, signalIndex - Math.max(1, lookbackBars) + 1);
    int activeDir = 0;
    int entryIndex = -1;
    double entry = Double.NaN;
    double sl = Double.NaN;
    double tp1 = Double.NaN;
    double tp2 = Double.NaN;
    double tp3 = Double.NaN;
    boolean tp1Reached = false;
    boolean tp2Reached = false;
    for (int i = start; i <= signalIndex && i < s.size(); i++) {
      if (!s.isBarComplete(i)) continue;
      if (activeDir != 0 && i > entryIndex) {
        double adverse = activeDir == 1 ? s.getLow(i) : s.getHigh(i);
        updateDrawdown(stats, stats.netPoints + activeDir * (adverse - entry));
        boolean slHit = activeDir == 1 ? s.getLow(i) <= sl : s.getHigh(i) >= sl;
        boolean tp1First = targetHitBeforeStop(activeDir, s.getOpen(i), s.getHigh(i), s.getLow(i), s.getClose(i), sl, tp1) && !tp1Reached;
        boolean tp2First = targetHitBeforeStop(activeDir, s.getOpen(i), s.getHigh(i), s.getLow(i), s.getClose(i), sl, tp2) && !tp2Reached;
        boolean finalTargetFirst = cfg.singleTarget ? tp1First
          : targetHitBeforeStop(activeDir, s.getOpen(i), s.getHigh(i), s.getLow(i), s.getClose(i), sl, tp3);
        if (tp1First) {
          stats.tp1Hits++;
          tp1Reached = true;
          if (!cfg.singleTarget && cfg.useBreakEven) sl = entry;
        }
        if (!cfg.singleTarget && tp2First) {
          stats.tp2Hits++;
          tp2Reached = true;
        }
        if (finalTargetFirst) {
          if (!cfg.singleTarget) stats.tp3Hits++;
          closeBacktestTrade(stats, activeDir, entry, cfg.singleTarget ? tp1 : tp3);
          activeDir = 0;
          entryIndex = -1;
          continue;
        }
        if (slHit) {
          boolean beStop = Math.abs(sl - entry) < 0.0000001;
          closeBacktestTrade(stats, activeDir, entry, sl);
          if (!beStop) stats.stops++;
          activeDir = 0;
          entryIndex = -1;
          continue;
        }
      }
      if (activeDir == 0) {
        double risk = nz(atrRisk[i]) * cfg.slMultEff;
        if (risk <= 0) continue;
        boolean goLong = longSignals[i];
        boolean goShort = !goLong && shortSignals[i];
        if (!goLong && !goShort) continue;
        activeDir = goLong ? 1 : -1;
        entryIndex = i;
        entry = s.getClose(i);
        sl = entry - activeDir * risk;
        tp1 = entry + activeDir * risk * (cfg.singleTarget ? cfg.tpEff : cfg.tp1Eff);
        tp2 = cfg.singleTarget ? Double.NaN : entry + activeDir * risk * cfg.tp2Eff;
        tp3 = cfg.singleTarget ? Double.NaN : entry + activeDir * risk * cfg.tp3Eff;
        tp1Reached = false;
        tp2Reached = false;
      }
    }
    if (activeDir != 0) {
      stats.activeDir = activeDir;
      stats.activeEntry = entry;
      stats.activeUnrealized = activeDir * (s.getClose(signalIndex) - entry);
    }
    return stats;
  }

  static double profitFactor(BacktestStats stats) {
    double grossLossAbs = Math.abs(stats.grossLossPoints);
    return grossLossAbs == 0.0 ? (stats.grossWinPoints > 0.0 ? Double.POSITIVE_INFINITY : 0.0) : stats.grossWinPoints / grossLossAbs;
  }

  static double netProfitFactor(BacktestStats stats) {
    double grossLossAbs = Math.abs(stats.grossLossPoints);
    return grossLossAbs == 0.0 ? (stats.netPoints > 0.0 ? Double.POSITIVE_INFINITY : 0.0) : stats.netPoints / grossLossAbs;
  }

  static double recoveryFactor(BacktestStats stats) {
    return stats.maxDrawdownPoints == 0.0 ? (stats.netPoints > 0.0 ? Double.POSITIVE_INFINITY : 0.0) : stats.netPoints / stats.maxDrawdownPoints;
  }

  static double boundedRatio(double value, double cap) {
    if (Double.isInfinite(value)) return cap;
    if (Double.isNaN(value)) return 0.0;
    return Math.clamp(value, -cap, cap);
  }

  static void updateDrawdown(BacktestStats stats, double equity) {
    if (equity > stats.peakPoints) stats.peakPoints = equity;
    double drawdown = stats.peakPoints - equity;
    if (drawdown > stats.maxDrawdownPoints) stats.maxDrawdownPoints = drawdown;
  }

  static void closeBacktestTrade(BacktestStats stats, int dir, double entry, double exit) {
    double pnl = dir * (exit - entry);
    stats.trades++;
    stats.netPoints += pnl;
    updateDrawdown(stats, stats.netPoints);
    if (pnl > 0) {
      stats.wins++;
      stats.grossWinPoints += pnl;
    }
    else if (pnl < 0) {
      stats.losses++;
      stats.grossLossPoints += pnl;
    }
    else {
      stats.breakEvens++;
    }
  }

  static double scoreBacktest(BacktestStats stats, SettingsView cfg, boolean enforceMinTrades) {
    if (stats.trades == 0 || stats.netPoints <= 0.0) return Double.NEGATIVE_INFINITY;
    if (enforceMinTrades && stats.trades < cfg.optimizerMinTrades) return Double.NEGATIVE_INFINITY;
    double pf = boundedRatio(profitFactor(stats), 10.0);
    double rf = boundedRatio(recoveryFactor(stats), 10.0);
    return switch (cfg.optimizerObjective) {
      case "Net Points" -> stats.netPoints + pf;
      case "Profit Factor" -> pf * 1000.0 + stats.netPoints;
      case "PF vs Max DD" -> pf * 900.0 + rf * 350.0 + stats.netPoints * 0.15 - stats.maxDrawdownPoints * 8.0;
      case "Recovery Factor" -> rf * 1000.0 + stats.netPoints;
      default -> stats.netPoints - stats.maxDrawdownPoints * 0.75 + Math.min(pf, 5.0) * 20.0 + Math.min(rf, 10.0) * 15.0;
    };
  }

  static int optimizerSignalStart(int signalIndex, SettingsView cfg) {
    return Math.max(0, signalIndex - Math.max(1, cfg.optimizerLookback) + 1);
  }

  static int optimizerWarmupBars(SettingsView cfg) {
    int indicatorWarmup = Math.max(Math.max(cfg.smaSlow, cfg.emaSlow), Math.max(cfg.macdSlow + cfg.macdSignal, cfg.adxLen + cfg.diLen));
    indicatorWarmup = Math.max(indicatorWarmup, Math.max(cfg.stochK + cfg.stochD + cfg.stochSmooth, cfg.smiLongPeriod + cfg.smiShortPeriod + cfg.smiSignalPeriod));
    indicatorWarmup = Math.max(indicatorWarmup, Math.max(cfg.bbLen, cfg.cciLen));
    indicatorWarmup = Math.max(indicatorWarmup, Math.max(cfg.atrRiskLen, cfg.tilsonPeriod));
    return Math.max(200, Math.max(cfg.swingLen * 6, indicatorWarmup * 3));
  }

  static boolean targetHitBeforeStop(int dir, double open, double high, double low, double close, double stop, double target) {
    if (dir == 0 || !finite(stop) || !finite(target)) return false;
    boolean targetHit = dir > 0 ? high >= target : low <= target;
    if (!targetHit) return false;
    boolean stopHit = dir > 0 ? low <= stop : high >= stop;
    if (!stopHit) return true;
    return firstHitOnOhlcPath(dir, open, high, low, close, stop, target) == 1;
  }

  private static int firstHitOnOhlcPath(int dir, double open, double high, double low, double close, double stop, double target) {
    boolean targetAtOpen = dir > 0 ? open >= target : open <= target;
    boolean stopAtOpen = dir > 0 ? open <= stop : open >= stop;
    if (targetAtOpen && stopAtOpen) return -1;
    if (targetAtOpen) return 1;
    if (stopAtOpen) return -1;

    // Without tick data, approximate the intrabar path by sending the first wick opposite the candle body.
    double[] path = close >= open
      ? new double[] { open, low, high, close }
      : new double[] { open, high, low, close };
    for (int i = 1; i < path.length; i++) {
      int hit = firstHitOnSegment(path[i - 1], path[i], stop, target);
      if (hit != 0) return hit;
    }
    return -1;
  }

  private static int firstHitOnSegment(double from, double to, double stop, double target) {
    boolean stopHit = crosses(from, to, stop);
    boolean targetHit = crosses(from, to, target);
    if (targetHit && stopHit) {
      double targetDistance = Math.abs(target - from);
      double stopDistance = Math.abs(stop - from);
      return targetDistance < stopDistance ? 1 : -1;
    }
    if (targetHit) return 1;
    if (stopHit) return -1;
    return 0;
  }

  private static boolean crosses(double from, double to, double level) {
    return (from <= level && level <= to) || (to <= level && level <= from);
  }

  private static boolean finite(double value) {
    return !Double.isNaN(value) && !Double.isInfinite(value);
  }

  private static double nz(double v) {
    return Double.isNaN(v) || Double.isInfinite(v) ? 0.0 : v;
  }
}
