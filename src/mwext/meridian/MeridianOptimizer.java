package mwext.meridian;

import static mwext.meridian.MeridianIndicators.*;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;

import java.util.Arrays;

final class MeridianOptimizer {
  static int maxCandidates(String depth) {
    return switch (depth) {
      case MeridianOptions.DEEP -> 1200;
      case MeridianOptions.MEDIUM -> 500;
      default -> 180;
    };
   }
   
  static int optimizerPasses(String depth) {
    return switch (depth) {
      case MeridianOptions.DEEP -> 3;
      case MeridianOptions.MEDIUM -> 2;
      default -> 1;
    };
  }

  static int optimizerPasses(SettingsView cfg) {
    if (cfg != null && MeridianOptions.AROUND_CURRENT.equals(cfg.optimizerSearch)) return 1;
    return optimizerPasses(cfg == null ? OptimizerDepthOption.FAST.label : cfg.optimizerDepth);
  }

  private String cacheKey = "";
  private OptimizerResult cache;
  private int lastComputeBar = -1;
  private String cachePlanKey = "";
  private MeridianIndicatorCache activeInputs;
  private String statusValue = "OFF";
  private String statusExtra = "optimizer disabled";
  private int statusKind;

  OptimizerResult getResult() {
    return cache;
  }
  OptimizerResult currentResult(DataSeries s, SettingsView cfg) {
    return cache != null && optimizerPlanKey(s, cfg).equals(cachePlanKey) ? cache : null;
  }

  void updateStatus(DataSeries s, SettingsView cfg, int signalIndex) {
    if (!cfg.showOptimizer && !cfg.autoApplyOptimizer) {
      setStatus("OFF", "optimizer disabled", 0);
      return;
    }
    if (cache == null) {
      setStatus("READY", "click Apply to run", 3);
      return;
    }
    if (!optimizerPlanKey(s, cfg).equals(cachePlanKey)) {
      setStatus("STALE", "settings/data changed; click Apply", 2);
      return;
    }
    setStatus("CACHED", cacheAgeText(cfg, signalIndex), 1);
  }

  OptimizerResult getResult(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex) {
    if (!cfg.showOptimizer && !cfg.autoApplyOptimizer) {
      setStatus("OFF", "optimizer disabled", 0);
      return null;
    }
    String planKey = optimizerPlanKey(s, cfg);
    boolean planMatches = cache != null && planKey.equals(cachePlanKey);
    if (cache == null && OptimizerRefreshOption.ON_DEMAND.label.equals(cfg.optRefreshMode) && !cfg.autoApplyOptimizer) {
      setStatus("READY", "click Apply to run", 3);
      return null;
    }
    if (cache != null && !planMatches && OptimizerRefreshOption.ON_DEMAND.label.equals(cfg.optRefreshMode) && !cfg.autoApplyOptimizer) {
      setStatus("STALE", "settings changed; click Apply", 2);
      return cache;
    }
    if (cache != null && planMatches && !shouldRecompute(cfg, signalIndex)) {
      setStatus("CACHED", cacheAgeText(cfg, signalIndex), 1);
      return cache;
    }
    return recompute(ctx, s, cfg, signalIndex, planKey);
  }

  OptimizerResult refresh(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex) {
    return recompute(ctx, s, cfg, signalIndex);
  }

  OptimizerResult apply(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex,
                        com.motivewave.platform.sdk.common.Settings st) {
    OptimizerResult result = recompute(ctx, s, cfg, signalIndex);
    st.setBoolean(MeridianFlowForge.APPLY_OPTIMIZER, false);
    if (result != null && result.valid && result.cfg != null) {
      applyOptimizerSettings(st, result.cfg);
      setStatus("APPLIED", result.candidates + " tries • " + result.computeMillis + "ms", 1);
    }
    return result;
  }

  void invalidate() {
    cache = null;
    cacheKey = "";
    cachePlanKey = "";
    lastComputeBar = -1;
    setStatus("RESET", "cache cleared", 2);
  }

  int lastComputeBar() {
    return lastComputeBar;
  }
  String statusValue() {
    return statusValue;
  }

  String statusExtra() {
    return statusExtra;
  }

  int statusKind() {
    return statusKind;
  }

  void markAutoApplied(OptimizerResult result) {
    if (result != null) setStatus("AUTO APPLIED", result.candidates + " tries • " + result.computeMillis + "ms", 1);
  }

  private boolean shouldRecompute(SettingsView cfg, int signalIndex) {
    int floor = optimizerRefreshFloor(cfg.optimizerDepth);
    int interval = switch (cfg.optRefreshMode) {
      case MeridianOptions.ON_DEMAND -> cfg.autoApplyOptimizer ? floor : Integer.MAX_VALUE;
      case MeridianOptions.EVERY_N_BARS -> Math.max(1, cfg.optRefreshInterval);
      case MeridianOptions.EVERY_BAR -> 1;
      default -> floor;
    };
    if (cache == null) return interval != Integer.MAX_VALUE;
    return (signalIndex - lastComputeBar) >= interval;
  }

  private static int optimizerRefreshFloor(String depth) {
    return switch (depth) {
      case MeridianOptions.DEEP -> 50;
      case MeridianOptions.MEDIUM -> 20;
      default -> 8;
    };
  }

  private OptimizerResult recompute(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex) {
    return recompute(ctx, s, cfg, signalIndex, optimizerPlanKey(s, cfg));
  }

  private OptimizerResult recompute(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex, String planKey) {
    String key = optimizerKey(s, cfg, signalIndex);
    if (cache != null && key.equals(cacheKey) && lastComputeBar == signalIndex) {
      setStatus("CACHED", "same bar • " + cacheAgeText(cfg, signalIndex), 1);
      return cache;
    }
    long start = System.nanoTime();
    OptimizerResult result = runOptimizer(ctx, s, cfg, signalIndex);
    result.computeMillis = Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
    result.computedAtBar = signalIndex;
    cache = result;
    cacheKey = key;
    cachePlanKey = planKey;
    lastComputeBar = signalIndex;
    setStatus(result.valid ? "UPDATED" : "NO RESULT", result.candidates + " tries • " + result.computeMillis + "ms", result.valid ? 1 : 2);
    return result;
  }

  private OptimizerResult runOptimizer(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex) {
    MeridianIndicatorCache priorInputs = activeInputs;
    activeInputs = new MeridianIndicatorCache(ctx, s);
    try {
      OptimizerAccumulator acc = new OptimizerAccumulator();
      SettingsView seed = cfg.copy();
      String depth = seed.optimizerDepth;
      int maxCand = maxCandidates(depth);
      int passes = optimizerPasses(seed);
      boolean iterative = OptimizerDepthOption.DEEP.label.equals(depth);

      // Always evaluate the user's current settings as a baseline candidate.
      considerOptimizerCandidate(acc, ctx, s, seed, signalIndex, maxCand);

      SettingsView anchor = seed.copy();
      for (int pass = 0; pass < passes && acc.candidates < maxCand; pass++) {
        scanAll(acc, ctx, s, anchor, signalIndex, maxCand);
        if (iterative && acc.best != null) {
          anchor = acc.best.cfg.copy();
        }
        else if (!iterative && acc.best != null) {
          anchor = acc.best.cfg.copy();
        }
      }


      OptimizerResult out = acc.best == null ? acc.fallback : acc.best;
      if (out == null) {
        out = new OptimizerResult();
        out.valid = false;
        out.note = "no positive candidates";
        out.stats = null;
        out.params = "-";
        out.objective = cfg.optimizerObjective;
      }
      out.candidates = acc.candidates;
      out.bars = Math.min(cfg.optimizerLookback, signalIndex + 1);
      out.objective = cfg.optimizerObjective;
      if (out.note == null && out.valid) {
        out.note = DashboardSupport.depthLabel(depth) + " · " + passes + " pass" + (passes > 1 ? "es" : "");
      }
      return out;
    }
    finally {
      activeInputs = priorInputs;
    }
  }

  private void scanAll(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor,
                       int signalIndex, int maxCand) {
    scanSwing(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSma(acc, ctx, s, anchor, signalIndex, maxCand);
    scanRsi(acc, ctx, s, anchor, signalIndex, maxCand);
    scanStoch(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSar(acc, ctx, s, anchor, signalIndex, maxCand);
    scanTilson(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSmi(acc, ctx, s, anchor, signalIndex, maxCand);
    scanRisk(acc, ctx, s, anchor, signalIndex, maxCand);
  }

  private void scanSwing(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    int[] values = uniqueInts(new int[] {anchor.swingLen - 4, anchor.swingLen - 2, anchor.swingLen - 1, anchor.swingLen,
      anchor.swingLen + 1, anchor.swingLen + 2, anchor.swingLen + 4, 3, 5, 8, 13, 21}, 2, 50);
    for (int value : values) {
      if (acc.candidates >= maxCand) return;
      SettingsView c = anchor.copy();
      c.swingLen = value;
      considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
    }
  }

  private void scanSma(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    if (!anchor.enableSma) return;
    int[] fasts = uniqueInts(new int[] {anchor.smaFast - 5, anchor.smaFast - 3, anchor.smaFast, anchor.smaFast + 3,
      anchor.smaFast + 5, 5, 8, 10, 13}, 1, 300);
    int[] slows = uniqueInts(new int[] {anchor.smaSlow - 10, anchor.smaSlow - 5, anchor.smaSlow, anchor.smaSlow + 5,
      anchor.smaSlow + 10, 20, 34, 50}, 2, 300);
    for (int fast : fasts) {
      for (int slow : slows) {
        if (acc.candidates >= maxCand) return;
        if (fast >= slow) continue;
        SettingsView c = anchor.copy();
        c.smaFast = fast;
        c.smaSlow = slow;
        considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
      }
    }
  }

  private void scanRsi(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    if (!anchor.enableRsi) return;
    int[] lens = uniqueInts(new int[] {anchor.rsiLen - 6, anchor.rsiLen - 3, anchor.rsiLen, anchor.rsiLen + 3,
      anchor.rsiLen + 6, 9, 14, 21}, 1, 200);
    double[] longs = uniqueDoubles(new double[] {anchor.rsiLong - 5.0, anchor.rsiLong, anchor.rsiLong + 5.0, 50.0, 55.0, 60.0}, 1.0, 99.0);
    double[] shorts = uniqueDoubles(new double[] {anchor.rsiShort - 5.0, anchor.rsiShort, anchor.rsiShort + 5.0, 40.0, 45.0, 50.0}, 1.0, 99.0);
    for (int len : lens) {
      for (double longLevel : longs) {
        for (double shortLevel : shorts) {
          if (acc.candidates >= maxCand) return;
          if (shortLevel > longLevel) continue;
          SettingsView c = anchor.copy();
          c.rsiLen = len;
          c.rsiLong = longLevel;
          c.rsiShort = shortLevel;
          considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
        }
      }
    }
  }

  private void scanStoch(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    if (!anchor.enableStoch) return;
    int[] ks = uniqueInts(new int[] {anchor.stochK - 5, anchor.stochK, anchor.stochK + 5, 9, 14, 21}, 1, 200);
    int[] ds = uniqueInts(new int[] {anchor.stochD, 3, 5, 8}, 1, 50);
    int[] smooths = uniqueInts(new int[] {anchor.stochSmooth, 3, 5, 8}, 1, 50);
    for (int k : ks) {
      for (int d : ds) {
        for (int smooth : smooths) {
          if (acc.candidates >= maxCand) return;
          SettingsView c = anchor.copy();
          c.stochK = k;
          c.stochD = d;
          c.stochSmooth = smooth;
          considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
        }
      }
    }
  }

  private void scanSar(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    if (!anchor.enableSar) return;
    double[] starts = uniqueDoubles(new double[] {anchor.sarStart, 0.01, 0.02, 0.03, 0.04}, 0.001, 1.0);
    double[] incs = uniqueDoubles(new double[] {anchor.sarInc, 0.01, 0.02, 0.03, 0.04}, 0.001, 1.0);
    double[] maxes = uniqueDoubles(new double[] {anchor.sarMax, 0.10, 0.20, 0.30, 0.40}, 0.01, 2.0);
    for (double start : starts) {
      for (double inc : incs) {
        for (double max : maxes) {
          if (acc.candidates >= maxCand) return;
          if (start > max || inc > max) continue;
          SettingsView c = anchor.copy();
          c.sarStart = start;
          c.sarInc = inc;
          c.sarMax = max;
          considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
        }
      }
    }
  }

  private void scanTilson(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    if (!anchor.enableTilson) return;
    int[] periods = uniqueInts(new int[] {anchor.tilsonPeriod - 3, anchor.tilsonPeriod - 1, anchor.tilsonPeriod,
      anchor.tilsonPeriod + 1, anchor.tilsonPeriod + 3, 7, 9, 10, 12}, 2, 200);
    String[] inputs = uniqueStrings(anchor.tilsonInput, MeridianOptions.HIGH, MeridianOptions.CLOSE, MeridianOptions.OPEN);
    String[] methods = uniqueStrings(anchor.tilsonMethod, MeridianOptions.MEMA, MeridianOptions.SMA, MeridianOptions.EMA);
    for (String input : inputs) {
      for (String method : methods) {
        for (int period : periods) {
          if (acc.candidates >= maxCand) return;
          SettingsView c = anchor.copy();
          c.tilsonInput = input;
          c.tilsonMethod = method;
          c.tilsonPeriod = period;
          considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
        }
      }
    }
  }

  private void scanSmi(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    if (!anchor.enableSmi) return;
    int[][] profiles = new int[][] {
      {anchor.smiLongPeriod, anchor.smiShortPeriod, anchor.smiSignalPeriod},
      {10, 12, 6},
      {12, 9, 7},
      {5, 11, 11},
      {11, 5, 11},
      {7, 11, 7},
      {14, 5, 5}
    };
    String[] inputs = uniqueStrings(anchor.smiInput, MeridianOptions.OPEN, MeridianOptions.CLOSE);
    String[] methods = uniqueStrings(anchor.smiMethod, MeridianOptions.SMA, MeridianOptions.EMA, MeridianOptions.MEMA);
    String[] modes = uniqueStrings(anchor.smiMode, SmiModeOption.LINE_VS_SIGNAL.label, SmiModeOption.ZERO_BIAS.label);
    for (String input : inputs) {
      for (String method : methods) {
        for (String mode : modes) {
          for (int[] profile : profiles) {
            if (acc.candidates >= maxCand) return;
            SettingsView c = anchor.copy();
            c.smiInput = input;
            c.smiMethod = method;
            c.smiMode = mode;
            c.smiLongPeriod = profile[0];
            c.smiShortPeriod = profile[1];
            c.smiSignalPeriod = profile[2];
            considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
          }
        }
      }
    }
  }

  private void scanRisk(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    double[][] profiles = new double[][] {
      {anchor.atrRiskLen, anchor.slMultEff, anchor.tpEff, anchor.tp1Eff, anchor.tp2Eff, anchor.tp3Eff},
      {8.0, 0.8, 1.5, 0.8, 1.5, 2.0},
      {10.0, 1.0, 2.0, 1.0, 2.0, 3.0},
      {13.0, 1.0, 2.5, 1.5, 2.5, 4.0},
      {13.0, 1.5, 2.0, 1.0, 2.0, 3.0},
      {20.0, 2.0, 2.0, 1.0, 2.0, 4.0}
    };
    for (double[] profile : profiles) {
      if (acc.candidates >= maxCand) return;
      SettingsView c = anchor.copy();
      c.riskPreset = RiskPresetOption.CUSTOM.label;
      c.atrRiskLen = (int)profile[0];
      c.slMultEff = profile[1];
      c.tpEff = profile[2];
      c.tp1Eff = profile[3];
      c.tp2Eff = profile[4];
      c.tp3Eff = profile[5];
      considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
    }
  }


  private void considerOptimizerCandidate(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView candidate, int signalIndex, int maxCand) {
    if (acc.candidates >= maxCand) return;
    if (candidate.enableSma && candidate.smaFast >= candidate.smaSlow) return;
    if (candidate.enableEma && candidate.emaFast >= candidate.emaSlow) return;
    if (candidate.enableMacd && candidate.macdFast >= candidate.macdSlow) return;
    acc.candidates++;
    int signalStart = MeridianBacktest.optimizerSignalStart(signalIndex, candidate);
    MeridianIndicatorCache inputs = activeInputs;
    SignalArrays signals = inputs == null
      ? buildOptimizationSignals(ctx, s, candidate, signalIndex, signalStart)
      : buildOptimizationSignals(inputs, candidate, signalIndex, signalStart);
    double[] atrRisk = inputs == null ? atr(s, candidate.atrRiskLen) : inputs.atr(candidate.atrRiskLen);
    BacktestStats stats = MeridianBacktest.runBacktest(s, candidate, signalIndex, signals.longs, signals.shorts, atrRisk, candidate.optimizerLookback);
    double fallbackScore = MeridianBacktest.scoreBacktest(stats, candidate, false);
    if (fallbackScore > Double.NEGATIVE_INFINITY && (acc.fallback == null || fallbackScore > acc.fallback.score)) {
      boolean lowTrades = stats.trades < candidate.optimizerMinTrades;
      acc.fallback = makeOptimizerResult(candidate, stats, fallbackScore, !lowTrades, lowTrades ? "below min trades" : null);
    }
    double score = MeridianBacktest.scoreBacktest(stats, candidate, true);
    if (score > Double.NEGATIVE_INFINITY && (acc.best == null || score > acc.best.score)) {
      acc.best = makeOptimizerResult(candidate, stats, score, true, null);
    }
  }

  SignalArrays buildOptimizationSignals(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex, int startIndex) {
    return buildOptimizationSignals(new MeridianIndicatorCache(ctx, s), cfg, signalIndex, startIndex);
  }

  private SignalArrays buildOptimizationSignals(MeridianIndicatorCache inputs, SettingsView cfg, int signalIndex, int startIndex) {
    DataSeries s = inputs.s;
    int n = inputs.n;
    int evalStart = Math.max(0, startIndex);
    int scanStart = Math.max(0, evalStart - MeridianBacktest.optimizerWarmupBars(cfg));
    double[] closes = inputs.closes;
    SignalSourceOption source = SignalSourceOption.from(cfg.signalSource);
    boolean needsForge = source.usesForge();
    double[] smaFast = needsForge && cfg.enableSma ? inputs.sma(cfg.smaFast) : null;
    double[] smaSlow = needsForge && cfg.enableSma ? inputs.sma(cfg.smaSlow) : null;
    double[] emaFast = needsForge && cfg.enableEma ? inputs.ema(cfg.emaFast) : null;
    double[] emaSlow = needsForge && cfg.enableEma ? inputs.ema(cfg.emaSlow) : null;
    double[] rsi = needsForge && cfg.enableRsi ? inputs.rsi(cfg.rsiLen) : null;
    Macd macd = needsForge && cfg.enableMacd ? inputs.macd(cfg.macdFast, cfg.macdSlow, cfg.macdSignal) : null;
    Stoch stoch = needsForge && cfg.enableStoch ? inputs.stoch(cfg.stochK, cfg.stochD, cfg.stochSmooth) : null;
    Bands bb = needsForge && cfg.enableBb ? inputs.bollinger(cfg.bbLen, cfg.bbMult) : null;
    double[] ao = needsForge && cfg.enableAo ? inputs.ao() : null;
    Sar sar = needsForge && cfg.enableSar ? inputs.sar(cfg.sarStart, cfg.sarInc, cfg.sarMax) : null;
    double[] cci = needsForge && cfg.enableCci ? inputs.cci(cfg.cciLen) : null;
    Adx adx = needsForge && cfg.enableAdx ? inputs.adx(cfg.diLen, cfg.adxLen) : null;
    Super superTrend = needsForge && cfg.enableSt ? inputs.superTrend(cfg.stLen, cfg.stFactor) : null;
    Tilson tilson = needsForge && cfg.enableTilson ? inputs.tilson(cfg.tilsonInput, cfg.tilsonMethod, cfg.tilsonPeriod) : null;
    Smi smi = needsForge && cfg.enableSmi ? inputs.smi(cfg.smiInput, cfg.smiMethod, cfg.smiLongPeriod, cfg.smiShortPeriod, cfg.smiSignalPeriod) : null;
    HtfBias htfBias = inputs.htfBias(cfg);
    boolean[] longs = new boolean[n];
    boolean[] shorts = new boolean[n];

    int warmup = Math.max(cfg.swingLen * 2, 50);
    double lastSwingHigh = Double.NaN;
    int lastSwingHighBar = -1;
    double lastSwingLow = Double.NaN;
    int lastSwingLowBar = -1;
    boolean swingHighBroken = true;
    boolean swingLowBroken = true;
    int structTrend = 0;
    boolean prevForgeLong = false;
    boolean prevForgeShort = false;

    for (int i = scanStart; i <= signalIndex && i < n; i++) {
      boolean confirmed = s.isBarComplete(i);
      int pivotBar = i - cfg.swingLen;
      if (pivotBar >= cfg.swingLen && pivotBar + cfg.swingLen <= signalIndex) {
        if (isPivotHigh(s, pivotBar, cfg.swingLen)) {
          lastSwingHigh = s.getHigh(pivotBar);
          lastSwingHighBar = pivotBar;
          swingHighBroken = false;
        }
        if (isPivotLow(s, pivotBar, cfg.swingLen)) {
          lastSwingLow = s.getLow(pivotBar);
          lastSwingLowBar = pivotBar;
          swingLowBroken = false;
        }
      }

      double breakHighSrc = cfg.breakOnWick ? s.getHigh(i) : s.getClose(i);
      double breakLowSrc = cfg.breakOnWick ? s.getLow(i) : s.getClose(i);
      boolean rawBullBreak = !Double.isNaN(lastSwingHigh) && lastSwingHighBar >= 0 && !swingHighBroken && breakHighSrc > lastSwingHigh;
      boolean rawBearBreak = !Double.isNaN(lastSwingLow) && lastSwingLowBar >= 0 && !swingLowBroken && breakLowSrc < lastSwingLow;
      boolean conflict = rawBullBreak && rawBearBreak;
      boolean bullBreak = rawBullBreak && !conflict && confirmed && i >= warmup;
      boolean bearBreak = rawBearBreak && !conflict && confirmed && i >= warmup;
      boolean isBullBos = false, isBullChoch = false, isBearBos = false, isBearChoch = false;
      if (bullBreak) {
        isBullBos = structTrend >= 0;
        isBullChoch = structTrend < 0;
        structTrend = 1;
        swingHighBroken = true;
      }
      if (bearBreak) {
        isBearBos = structTrend <= 0;
        isBearChoch = structTrend > 0;
        structTrend = -1;
        swingLowBroken = true;
      }

      boolean bullEvent = eventAllowed(cfg.signalMode, isBullBos, isBullChoch);
      boolean bearEvent = eventAllowed(cfg.signalMode, isBearBos, isBearChoch);
      boolean htfBullOk = !cfg.useHtf || htfBias.bull[i];
      boolean htfBearOk = !cfg.useHtf || htfBias.bear[i];
      boolean forgeLong = false;
      boolean forgeShort = false;
      if (needsForge) {
        ForgeState state = forgeState(cfg, i, smaFast, smaSlow, emaFast, emaSlow, rsi, macd, stoch, bb,
          closes, ao, sar, cci, adx, superTrend, tilson, smi);
        forgeLong = state.longOk;
        forgeShort = state.shortOk;
      }
      boolean forgeLongRising = forgeLong && !prevForgeLong;
      boolean forgeShortRising = forgeShort && !prevForgeShort;
      longs[i] = switch (source) {
        case STRUCTURE_ONLY -> bullEvent && htfBullOk;
        case FORGE_ONLY -> forgeLongRising && htfBullOk;
        default -> bullEvent && htfBullOk && forgeLong;
      };
      shorts[i] = switch (source) {
        case STRUCTURE_ONLY -> bearEvent && htfBearOk;
        case FORGE_ONLY -> forgeShortRising && htfBearOk;
        default -> bearEvent && htfBearOk && forgeShort;
      };

      if (longs[i] && shorts[i]) {
        longs[i] = false;
        shorts[i] = false;
      }
      prevForgeLong = forgeLong;
      prevForgeShort = forgeShort;
    }
    return new SignalArrays(longs, shorts);
  }

  static OptimizerResult makeOptimizerResult(SettingsView cfg, BacktestStats stats, double score, boolean valid, String note) {
    OptimizerResult out = new OptimizerResult();
    out.valid = valid;
    out.score = score;
    out.stats = stats;
    out.cfg = cfg.copy();
    out.params = parameterSummary(cfg);
    out.note = note;
    return out;
  }

  static String optimizerKey(DataSeries s, SettingsView cfg, int signalIndex) {
    StringBuilder b = new StringBuilder(512);
    long signalTime = signalIndex >= 0 && signalIndex < s.size() ? s.getStartTime(signalIndex) : 0L;
    b.append(signalIndex).append('|').append(s.size()).append('|').append(signalTime).append('|').append(s.getBarSize());
    if (signalIndex >= 0 && signalIndex < s.size()) {
      b.append('|').append(s.getOpen(signalIndex)).append('|').append(s.getHigh(signalIndex))
        .append('|').append(s.getLow(signalIndex)).append('|').append(s.getClose(signalIndex));
    }
    cfg.appendOptimizerSettings(b);
    return b.toString();
  }

  static String optimizerPlanKey(DataSeries s, SettingsView cfg) {
    StringBuilder b = new StringBuilder(512);
    b.append(s.getBarSize()).append('|').append(s.size() == 0 ? 0L : s.getStartTime(0));
    cfg.appendOptimizerSettings(b);
    return b.toString();
  }

  static void applyOptimizerSettings(com.motivewave.platform.sdk.common.Settings st, SettingsView c) {
    st.setInteger(MeridianFlowForge.SWING_LEN, c.swingLen);
    st.setString(MeridianFlowForge.SIGNAL_MODE, c.signalMode);
    st.setString(MeridianFlowForge.SIGNAL_SOURCE, c.signalSource);
    st.setString(MeridianFlowForge.RISK_PRESET, c.riskPreset);
    st.setString(MeridianFlowForge.TP_MODE, c.tpMode);
    st.setInteger(MeridianFlowForge.ATR_RISK_LEN, c.atrRiskLen);
    if (RiskPresetOption.CUSTOM.label.equals(c.riskPreset)) {
      st.setDouble(MeridianFlowForge.SL_MULT, c.slMultEff);
      st.setDouble(MeridianFlowForge.TP_MULT, c.tpEff);
      st.setDouble(MeridianFlowForge.TP1_MULT, c.tp1Eff);
      st.setDouble(MeridianFlowForge.TP2_MULT, c.tp2Eff);
      st.setDouble(MeridianFlowForge.TP3_MULT, c.tp3Eff);
    }
    st.setBoolean(MeridianFlowForge.USE_BE, c.useBreakEven);
    st.setBoolean(MeridianFlowForge.REQUIRE_ALL, c.requireAll);
    st.setBoolean(MeridianFlowForge.ENABLE_SMA, c.enableSma);
    st.setInteger(MeridianFlowForge.SMA_FAST, c.smaFast);
    st.setInteger(MeridianFlowForge.SMA_SLOW, c.smaSlow);
    st.setBoolean(MeridianFlowForge.ENABLE_RSI, c.enableRsi);
    st.setInteger(MeridianFlowForge.RSI_LEN, c.rsiLen);
    st.setDouble(MeridianFlowForge.RSI_LONG, c.rsiLong);
    st.setDouble(MeridianFlowForge.RSI_SHORT, c.rsiShort);
    st.setBoolean(MeridianFlowForge.ENABLE_MACD, c.enableMacd);
    st.setInteger(MeridianFlowForge.MACD_FAST, c.macdFast);
    st.setInteger(MeridianFlowForge.MACD_SLOW, c.macdSlow);
    st.setInteger(MeridianFlowForge.MACD_SIGNAL, c.macdSignal);
    st.setBoolean(MeridianFlowForge.ENABLE_ST, c.enableSt);
    st.setInteger(MeridianFlowForge.ST_LEN, c.stLen);
    st.setDouble(MeridianFlowForge.ST_FACTOR, c.stFactor);
    st.setBoolean(MeridianFlowForge.ENABLE_STOCH, c.enableStoch);
    st.setInteger(MeridianFlowForge.STOCH_K, c.stochK);
    st.setInteger(MeridianFlowForge.STOCH_D, c.stochD);
    st.setInteger(MeridianFlowForge.STOCH_SMOOTH, c.stochSmooth);
    st.setBoolean(MeridianFlowForge.ENABLE_SAR, c.enableSar);
    st.setDouble(MeridianFlowForge.SAR_START, c.sarStart);
    st.setDouble(MeridianFlowForge.SAR_INC, c.sarInc);
    st.setDouble(MeridianFlowForge.SAR_MAX, c.sarMax);
    st.setBoolean(MeridianFlowForge.ENABLE_TILSON, c.enableTilson);
    st.setString(MeridianFlowForge.TILSON_INPUT, c.tilsonInput);
    st.setString(MeridianFlowForge.TILSON_METHOD, c.tilsonMethod);
    st.setInteger(MeridianFlowForge.TILSON_PERIOD, c.tilsonPeriod);
    st.setBoolean(MeridianFlowForge.ENABLE_SMI, c.enableSmi);
    st.setString(MeridianFlowForge.SMI_INPUT, c.smiInput);
    st.setString(MeridianFlowForge.SMI_METHOD, c.smiMethod);
    st.setString(MeridianFlowForge.SMI_MODE, c.smiMode);
    st.setInteger(MeridianFlowForge.SMI_LONG_PERIOD, c.smiLongPeriod);
    st.setInteger(MeridianFlowForge.SMI_SHORT_PERIOD, c.smiShortPeriod);
    st.setInteger(MeridianFlowForge.SMI_SIGNAL_PERIOD, c.smiSignalPeriod);
  }

  private static int[] uniqueInts(int[] values, int min, int max) {
    int[] tmp = new int[values.length];
    int count = 0;
    for (int value : values) {
      int v = Math.max(min, Math.min(max, value));
      boolean seen = false;
      for (int i = 0; i < count; i++) {
        if (tmp[i] == v) { seen = true; break; }
      }
      if (!seen) tmp[count++] = v;
    }
    int[] out = Arrays.copyOf(tmp, count);
    Arrays.sort(out);
    return out;
  }

  private static double[] uniqueDoubles(double[] values, double min, double max) {
    double[] tmp = new double[values.length];
    int count = 0;
    for (double value : values) {
      double v = Math.max(min, Math.min(max, Math.round(value * 1000.0) / 1000.0));
      boolean seen = false;
      for (int i = 0; i < count; i++) {
        if (Math.abs(tmp[i] - v) < 0.0000001) { seen = true; break; }
      }
      if (!seen) tmp[count++] = v;
    }
    double[] out = Arrays.copyOf(tmp, count);
    Arrays.sort(out);
    return out;
  }

  private static String[] uniqueStrings(String... values) {
    String[] tmp = new String[values.length];
    int count = 0;
    for (String value : values) {
      if (value == null || value.isEmpty()) continue;
      boolean seen = false;
      for (int i = 0; i < count; i++) {
        if (tmp[i].equals(value)) { seen = true; break; }
      }
      if (!seen) tmp[count++] = value;
    }
    return Arrays.copyOf(tmp, count);
  }

  private static boolean eventAllowed(String mode, boolean bos, boolean choch) {
    return SignalModeOption.eventAllowed(mode, bos, choch);
  }

  private static boolean isPivotHigh(DataSeries s, int pivot, int len) {
    double v = s.getHigh(pivot);
    for (int i = pivot - len; i <= pivot + len; i++) {
      if (i == pivot) continue;
      if (i < 0 || i >= s.size()) return false;
      if (s.getHigh(i) > v) return false;
    }
    return true;
  }

  private static boolean isPivotLow(DataSeries s, int pivot, int len) {
    double v = s.getLow(pivot);
    for (int i = pivot - len; i <= pivot + len; i++) {
      if (i == pivot) continue;
      if (i < 0 || i >= s.size()) return false;
      if (s.getLow(i) < v) return false;
    }
    return true;
  }

  private String cacheAgeText(SettingsView cfg, int signalIndex) {
    int barsAgo = lastComputeBar < 0 ? 0 : Math.max(0, signalIndex - lastComputeBar);
    int interval = refreshInterval(cfg);
    if (interval == Integer.MAX_VALUE) return barsAgo + " bars ago • on demand";
    int next = Math.max(0, interval - barsAgo);
    return barsAgo + " bars ago • next " + next;
  }

  private int refreshInterval(SettingsView cfg) {
    int floor = optimizerRefreshFloor(cfg.optimizerDepth);
    return switch (cfg.optRefreshMode) {
      case MeridianOptions.ON_DEMAND -> cfg.autoApplyOptimizer ? floor : Integer.MAX_VALUE;
      case MeridianOptions.EVERY_N_BARS -> Math.max(1, cfg.optRefreshInterval);
      case MeridianOptions.EVERY_BAR -> 1;
      default -> floor;
    };
  }

  private void setStatus(String value, String extra, int kind) {
    statusValue = value == null ? "" : value;
    statusExtra = extra == null ? "" : extra;
    statusKind = kind;
  }
  private static String parameterSummary(SettingsView cfg) {
    String filters = DashboardSupport.parameterSummaryFilters(cfg, true);
    return DashboardSupport.parameterSummaryCore(cfg) + " | " + DashboardSupport.parameterSummaryRisk(cfg) + (filters.isEmpty() ? "" : " | " + filters);
  }
}
