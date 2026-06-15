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
        boolean tp1Hit = !Double.isNaN(tp1) && (activeDir == 1 ? s.getHigh(i) >= tp1 : s.getLow(i) <= tp1);
        boolean tp2Hit = !Double.isNaN(tp2) && (activeDir == 1 ? s.getHigh(i) >= tp2 : s.getLow(i) <= tp2);
        boolean tp3Hit = !Double.isNaN(tp3) && (activeDir == 1 ? s.getHigh(i) >= tp3 : s.getLow(i) <= tp3);
        boolean tp1First = targetHitBeforeStop(slHit, tp1Hit) && !tp1Reached;
        boolean tp2First = targetHitBeforeStop(slHit, tp2Hit) && !tp2Reached;
        boolean finalTargetFirst = targetHitBeforeStop(slHit, cfg.singleTarget ? tp1Hit : tp3Hit);
        if (slHit) {
          boolean beStop = Math.abs(sl - entry) < 0.0000001;
          closeBacktestTrade(stats, activeDir, entry, sl);
          if (!beStop) stats.stops++;
          activeDir = 0;
          entryIndex = -1;
          continue;
        }
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

  static boolean targetHitBeforeStop(boolean slHit, boolean targetHit) {
    return targetHit && !slHit;
  }

  private static double nz(double v) {
    return Double.isNaN(v) || Double.isInfinite(v) ? 0.0 : v;
  }
}
