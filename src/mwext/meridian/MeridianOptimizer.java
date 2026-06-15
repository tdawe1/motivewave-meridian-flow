package mwext.meridian;

import static mwext.meridian.MeridianIndicators.*;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

final class MeridianOptimizer {
  static int maxCandidates(String depth) {
    return switch (depth) {
      case "Deep" -> 2500;
      case "Medium" -> 1200;
      default -> 560;
    };
   }
   
  static int optimizerPasses(String depth) {
    return switch (depth) {
      case "Deep" -> 3;
      case "Medium" -> 2;
      default -> 1;
    };
  }

  private String cacheKey = "";
  private OptimizerResult cache;
  private int lastComputeBar = -1;
  private String cachePlanKey = "";
  private OptimizerInputs activeInputs;

  OptimizerResult getResult() {
    return cache;
  }

  OptimizerResult getResult(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex) {
    if (!cfg.showOptimizer && !cfg.autoApplyOptimizer) return null;
    String planKey = optimizerPlanKey(s, cfg);
    if (cache != null && planKey.equals(cachePlanKey) && !shouldRecompute(cfg, signalIndex)) return cache;
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
    }
    return result;
  }

  void invalidate() {
    cache = null;
    cacheKey = "";
    cachePlanKey = "";
    lastComputeBar = -1;
  }

  int lastComputeBar() {
    return lastComputeBar;
  }

  private boolean shouldRecompute(SettingsView cfg, int signalIndex) {
    int floor = optimizerRefreshFloor(cfg.optimizerDepth);
    int interval = switch (cfg.optRefreshMode) {
      case "On Demand" -> cfg.autoApplyOptimizer ? floor : Integer.MAX_VALUE;
      case "Every N Bars" -> Math.max(floor, cfg.optRefreshInterval);
      default -> floor;
    };
    if (cache == null) return interval != Integer.MAX_VALUE;
    return (signalIndex - lastComputeBar) >= interval;
  }

  private static int optimizerRefreshFloor(String depth) {
    return switch (depth) {
      case "Deep" -> 50;
      case "Medium" -> 20;
      default -> 8;
    };
  }

  private OptimizerResult recompute(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex) {
    return recompute(ctx, s, cfg, signalIndex, optimizerPlanKey(s, cfg));
  }

  private OptimizerResult recompute(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex, String planKey) {
    String key = optimizerKey(s, cfg, signalIndex);
    if (cache != null && key.equals(cacheKey) && lastComputeBar == signalIndex) return cache;
    OptimizerResult result = runOptimizer(ctx, s, cfg, signalIndex);
    cache = result;
    cacheKey = key;
    cachePlanKey = planKey;
    lastComputeBar = signalIndex;
    return result;
  }

  private OptimizerResult runOptimizer(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex) {
    OptimizerInputs priorInputs = activeInputs;
    activeInputs = new OptimizerInputs(ctx, s);
    try {
      OptimizerAccumulator acc = new OptimizerAccumulator();
      SettingsView seed = cfg.copy();
      String depth = seed.optimizerDepth;
      int maxCand = maxCandidates(depth);
      int passes = optimizerPasses(depth);
      boolean iterative = "Deep".equals(depth);

      // Always evaluate the user's current settings as a baseline candidate.
      considerOptimizerCandidate(acc, ctx, s, seed, signalIndex, maxCand);

      SettingsView anchor = seed.copy();
      for (int pass = 0; pass < passes && acc.candidates < maxCand; pass++) {
        boolean multiFamily = iterative && pass >= 1;
        scanAll(acc, ctx, s, anchor, signalIndex, maxCand, multiFamily);
        if (iterative && acc.best != null) {
          anchor = acc.best.cfg.copy();
        }
        else if (!iterative && acc.best != null) {
          anchor = acc.best.cfg.copy();
        }
      }

      if ("Manual".equals(seed.signalGroup)) {
        scanBundles(acc, ctx, s, seed, signalIndex, maxCand, depth);
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
                       int signalIndex, int maxCand, boolean multiFamily) {
    scanSwing(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSma(acc, ctx, s, anchor, signalIndex, maxCand);
    scanRsi(acc, ctx, s, anchor, signalIndex, maxCand);
    scanStoch(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSar(acc, ctx, s, anchor, signalIndex, maxCand);
    scanTilson(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSmi(acc, ctx, s, anchor, signalIndex, maxCand);
    scanRisk(acc, ctx, s, anchor, signalIndex, maxCand);
    if (multiFamily) {
      scanMultiFamilyCross(acc, ctx, s, anchor, signalIndex, maxCand);
    }
  }

  private void scanSwing(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView anchor, int signalIndex, int maxCand) {
    int[] values = uniqueInts(new int[] {anchor.swingLen - 6, anchor.swingLen - 4, anchor.swingLen - 2, anchor.swingLen,
      anchor.swingLen + 2, anchor.swingLen + 4, anchor.swingLen + 6, 8, 13, 21}, 2, 50);
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
    String[] inputs = uniqueStrings(anchor.tilsonInput, "High", "Close", "Open");
    String[] methods = uniqueStrings(anchor.tilsonMethod, "MEMA", "SMA", "EMA");
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
    String[] inputs = uniqueStrings(anchor.smiInput, "Open", "Close");
    String[] methods = uniqueStrings(anchor.smiMethod, "SMA", "EMA", "MEMA");
    String[] modes = uniqueStrings(anchor.smiMode, "Line vs Signal", "Zero Bias");
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
      c.riskPreset = "Custom";
      c.atrRiskLen = (int)profile[0];
      c.slMultEff = profile[1];
      c.tpEff = profile[2];
      c.tp1Eff = profile[3];
      c.tp2Eff = profile[4];
      c.tp3Eff = profile[5];
      considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
    }
  }

  // --- Coherent bundle support ---

  /** Apply a named signal group bundle to a candidate, then evaluate it. */
  private void applyCandidateBundle(OptimizerAccumulator acc, DataContext ctx, DataSeries s,
                                    SettingsView seed, String bundleName, int signalIndex, int maxCand) {
    SettingsView c = seed.copy();
    SettingsView.applySignalGroup(c, bundleName);
    c.signalGroup = bundleName;
    // Ensure signal source is appropriate for the bundle
    if ("Structure Only".equals(bundleName)) {
      c.signalSource = "Structure only";
    }
    considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
  }

  /** Scan all coherent bundles in Manual mode, with depth-appropriate granularity. */
  private void scanBundles(OptimizerAccumulator acc, DataContext ctx, DataSeries s,
                           SettingsView seed, int signalIndex, int maxCand, String depth) {
    String[] groups = {"Trend Confirmation", "Momentum Pullback", "Mean Reversion", "Structure Only", "Balanced"};
    // Pass 1: evaluate each bundle as-is with user's default params
    for (String group : groups) {
      if (acc.candidates >= maxCand) return;
      applyCandidateBundle(acc, ctx, s, seed, group, signalIndex, maxCand);
    }
    if (!"Fast".equals(depth)) {
      for (String group : groups) {
        if (acc.candidates >= maxCand) return;
        SettingsView bundleCfg = seed.copy();
        SettingsView.applySignalGroup(bundleCfg, group);
        bundleCfg.signalGroup = group;
        scanWithinBundle(acc, ctx, s, bundleCfg, signalIndex, maxCand);
      }
    }
  }

  /** Scan parameter variations within a single coherent bundle, respecting its indicator set. */
  private void scanWithinBundle(OptimizerAccumulator acc, DataContext ctx, DataSeries s,
                                SettingsView anchor, int signalIndex, int maxCand) {
    scanSwing(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSma(acc, ctx, s, anchor, signalIndex, maxCand);
    scanRsi(acc, ctx, s, anchor, signalIndex, maxCand);
    scanStoch(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSar(acc, ctx, s, anchor, signalIndex, maxCand);
    scanTilson(acc, ctx, s, anchor, signalIndex, maxCand);
    scanSmi(acc, ctx, s, anchor, signalIndex, maxCand);
    scanRisk(acc, ctx, s, anchor, signalIndex, maxCand);
  }

  /** Deep-mode cross-family scans where multiple parameter families change together. */
  private void scanMultiFamilyCross(OptimizerAccumulator acc, DataContext ctx, DataSeries s,
                                    SettingsView anchor, int signalIndex, int maxCand) {
    // Cross-family bundle 1: RSI + Stochastic together (momentum cluster)
    if (anchor.enableRsi && anchor.enableStoch && acc.candidates < maxCand) {
      int[] rsiLens = uniqueInts(new int[] {anchor.rsiLen - 3, anchor.rsiLen, anchor.rsiLen + 3, 9, 14, 21}, 1, 200);
      int[] stochKs = uniqueInts(new int[] {anchor.stochK, 9, 14, 21}, 1, 200);
      for (int rl : rsiLens) {
        for (int sk : stochKs) {
          if (acc.candidates >= maxCand) return;
          SettingsView c = anchor.copy();
          c.rsiLen = rl;
          c.stochK = sk;
          considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
        }
      }
    }
    // Cross-family bundle 2: SMA + Supertrend together (trend cluster)
    if (anchor.enableSma && anchor.enableSt && acc.candidates < maxCand) {
      int[] smaFasts = uniqueInts(new int[] {anchor.smaFast - 3, anchor.smaFast, anchor.smaFast + 3, 5, 8, 13}, 1, 300);
      double[] stFactors = uniqueDoubles(new double[] {anchor.stFactor, 2.0, 4.0, 6.0}, 0.1, 20.0);
      for (int sf : smaFasts) {
        for (double stf : stFactors) {
          if (acc.candidates >= maxCand) return;
          SettingsView c = anchor.copy();
          c.smaFast = sf;
          c.stFactor = stf;
          considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
        }
      }
    }
    // Cross-family bundle 3: Risk + Swing together
    if (acc.candidates < maxCand) {
      int[] swings = uniqueInts(new int[] {anchor.swingLen - 4, anchor.swingLen, anchor.swingLen + 4, 8, 13, 21}, 2, 50);
      double[] slMults = uniqueDoubles(new double[] {anchor.slMultEff, 1.0, 1.5, 2.0, 2.5}, 0.5, 5.0);
      for (int sw : swings) {
        for (double sl : slMults) {
          if (acc.candidates >= maxCand) return;
          SettingsView c = anchor.copy();
          c.swingLen = sw;
          c.slMultEff = sl;
          c.riskPreset = "Custom";
          considerOptimizerCandidate(acc, ctx, s, c, signalIndex, maxCand);
        }
      }
    }
  }

  private void considerOptimizerCandidate(OptimizerAccumulator acc, DataContext ctx, DataSeries s, SettingsView candidate, int signalIndex, int maxCand) {
    if (acc.candidates >= maxCand) return;
    if (candidate.enableSma && candidate.smaFast >= candidate.smaSlow) return;
    if (candidate.enableEma && candidate.emaFast >= candidate.emaSlow) return;
    if (candidate.enableMacd && candidate.macdFast >= candidate.macdSlow) return;
    acc.candidates++;
    int signalStart = MeridianBacktest.optimizerSignalStart(signalIndex, candidate);
    OptimizerInputs inputs = activeInputs;
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
    return buildOptimizationSignals(new OptimizerInputs(ctx, s), cfg, signalIndex, startIndex);
  }

  private SignalArrays buildOptimizationSignals(OptimizerInputs inputs, SettingsView cfg, int signalIndex, int startIndex) {
    DataSeries s = inputs.s;
    int n = inputs.n;
    int evalStart = Math.max(0, startIndex);
    int scanStart = Math.max(0, evalStart - MeridianBacktest.optimizerWarmupBars(cfg));
    double[] closes = inputs.closes;
    boolean needsForge = !"Structure only".equals(cfg.signalSource);
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
      longs[i] = switch (cfg.signalSource) {
        case "Structure only" -> bullEvent && htfBullOk;
        case "Forge only" -> forgeLongRising && htfBullOk;
        default -> bullEvent && htfBullOk && forgeLong;
      };
      shorts[i] = switch (cfg.signalSource) {
        case "Structure only" -> bearEvent && htfBearOk;
        case "Forge only" -> forgeShortRising && htfBearOk;
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
    appendOptimizerSettings(b, cfg);
    return b.toString();
  }

  static String optimizerPlanKey(DataSeries s, SettingsView cfg) {
    StringBuilder b = new StringBuilder(512);
    b.append(s.getBarSize()).append('|').append(s.size() == 0 ? 0L : s.getStartTime(0));
    appendOptimizerSettings(b, cfg);
    return b.toString();
  }

  private static void appendOptimizerSettings(StringBuilder b, SettingsView cfg) {
    b.append('|').append(cfg.optimizerLookback).append('|').append(cfg.optimizerMinTrades).append('|').append(cfg.optimizerObjective).append('|').append(cfg.optimizerSearch).append('|').append(cfg.optimizerDepth);
    b.append('|').append(cfg.swingLen).append('|').append(cfg.breakOnWick).append('|').append(cfg.signalMode).append('|').append(cfg.signalSource);
    b.append('|').append(cfg.useHtf).append('|').append(cfg.htfBarSize).append('|').append(cfg.htfEmaLen).append('|').append(cfg.requireAll);
    b.append('|').append(cfg.enableSma).append('|').append(cfg.smaFast).append('|').append(cfg.smaSlow);
    b.append('|').append(cfg.enableRsi).append('|').append(cfg.rsiLen).append('|').append(cfg.rsiLong).append('|').append(cfg.rsiShort);
    b.append('|').append(cfg.enableMacd).append('|').append(cfg.macdFast).append('|').append(cfg.macdSlow).append('|').append(cfg.macdSignal);
    b.append('|').append(cfg.enableSt).append('|').append(cfg.stLen).append('|').append(cfg.stFactor);
    b.append('|').append(cfg.enableStoch).append('|').append(cfg.stochK).append('|').append(cfg.stochD).append('|').append(cfg.stochSmooth);
    b.append('|').append(cfg.enableBb).append('|').append(cfg.bbLen).append('|').append(cfg.bbMult);
    b.append('|').append(cfg.enableEma).append('|').append(cfg.emaFast).append('|').append(cfg.emaSlow);
    b.append('|').append(cfg.enableAo).append('|').append(cfg.enableSar).append('|').append(cfg.sarStart).append('|').append(cfg.sarInc).append('|').append(cfg.sarMax);
    b.append('|').append(cfg.enableCci).append('|').append(cfg.cciLen).append('|').append(cfg.cciLong).append('|').append(cfg.cciShort);
    b.append('|').append(cfg.enableAdx).append('|').append(cfg.diLen).append('|').append(cfg.adxLen).append('|').append(cfg.adxThreshold);
    b.append('|').append(cfg.enableTilson).append('|').append(cfg.tilsonInput).append('|').append(cfg.tilsonMethod).append('|').append(cfg.tilsonPeriod);
    b.append('|').append(cfg.enableSmi).append('|').append(cfg.smiInput).append('|').append(cfg.smiMethod).append('|').append(cfg.smiLongPeriod).append('|').append(cfg.smiShortPeriod).append('|').append(cfg.smiSignalPeriod)
      .append('|').append(cfg.smiTopGuide).append('|').append(cfg.smiBottomGuide).append('|').append(cfg.smiMode);
    b.append('|').append(cfg.atrRiskLen).append('|').append(cfg.slMultEff).append('|').append(cfg.tpEff).append('|').append(cfg.tp1Eff).append('|').append(cfg.tp2Eff).append('|').append(cfg.tp3Eff).append('|').append(cfg.riskPreset).append('|').append(cfg.tpMode).append('|').append(cfg.singleTarget).append('|').append(cfg.useBreakEven);
  }

  static void applyOptimizerSettings(com.motivewave.platform.sdk.common.Settings st, SettingsView c) {
    st.setInteger(MeridianFlowForge.SWING_LEN, c.swingLen);
    st.setString(MeridianFlowForge.SIGNAL_MODE, c.signalMode);
    st.setString(MeridianFlowForge.SIGNAL_SOURCE, c.signalSource);
    st.setString(MeridianFlowForge.RISK_PRESET, c.riskPreset);
    st.setString(MeridianFlowForge.TP_MODE, c.tpMode);
    st.setInteger(MeridianFlowForge.ATR_RISK_LEN, c.atrRiskLen);
    if ("Custom".equals(c.riskPreset)) {
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

  // --- Delegates to MeridianFlowForge helpers (same package, no access issues) ---

  private static HtfBias buildHtfBias(SettingsView cfg, DataContext ctx, DataSeries base, int n) {
    // Mirrors MeridianFlowForge.buildHtfBias for optimizer signal building.
    boolean[] bull = new boolean[n];
    boolean[] bear = new boolean[n];
    if (!cfg.useHtf) {
      Arrays.fill(bull, true);
      Arrays.fill(bear, true);
      return new HtfBias(bull, bear);
    }
    DataSeries htf = ctx.getDataSeries(cfg.htfBarSize);
    if (htf == null || htf.size() < cfg.htfEmaLen + 2) return new HtfBias(bull, bear);
    double[] htfEma = ema(htf, cfg.htfEmaLen);
    for (int i = 0; i < n; i++) {
      int hi = htf.findIndex(base.getStartTime(i));
      if (hi > 0) hi -= 1;
      if (hi < 0 || hi >= htfEma.length || Double.isNaN(htfEma[hi])) continue;
      double htfClose = htf.getClose(hi);
      bull[i] = htfClose >= htfEma[hi];
      bear[i] = htfClose <= htfEma[hi];
    }
    return new HtfBias(bull, bear);
  }

  private static boolean eventAllowed(String mode, boolean bos, boolean choch) {
    return switch (mode) {
      case "CHoCH only" -> choch;
      case "BOS only" -> bos;
      default -> bos || choch;
    };
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

  private static ForgeState forgeState(SettingsView c, int i, double[] smaFast, double[] smaSlow, double[] emaFast,
                                       double[] emaSlow, double[] rsi, Macd macd, Stoch stoch, Bands bb,
                                       double[] closes, double[] ao, Sar sar, double[] cciArr, Adx adx, Super st,
                                       Tilson tilson, Smi smi) {
    boolean longCond = c.requireAll;
    boolean shortCond = c.requireAll;
    boolean any = false;
    if (c.enableSma) { any = true; longCond = merge(c.requireAll, longCond, smaFast[i] > smaSlow[i]); shortCond = merge(c.requireAll, shortCond, smaFast[i] < smaSlow[i]); }
    if (c.enableRsi) { any = true; longCond = merge(c.requireAll, longCond, rsi[i] > c.rsiLong); shortCond = merge(c.requireAll, shortCond, rsi[i] < c.rsiShort); }
    if (c.enableMacd) { any = true; longCond = merge(c.requireAll, longCond, macd.line[i] > macd.signal[i]); shortCond = merge(c.requireAll, shortCond, macd.line[i] < macd.signal[i]); }
    if (c.enableSt) { any = true; longCond = merge(c.requireAll, longCond, st.dir[i] == -1); shortCond = merge(c.requireAll, shortCond, st.dir[i] == 1); }
    if (c.enableStoch) {
      any = true;
      boolean stochLong = !Double.isNaN(stoch.k[i]) && !Double.isNaN(stoch.d[i]) && stoch.k[i] > stoch.d[i] && stoch.k[i] > 50;
      boolean stochShort = !Double.isNaN(stoch.k[i]) && !Double.isNaN(stoch.d[i]) && stoch.k[i] < stoch.d[i] && stoch.k[i] < 50;
      longCond = merge(c.requireAll, longCond, stochLong);
      shortCond = merge(c.requireAll, shortCond, stochShort);
    }
    if (c.enableBb) {
      any = true;
      boolean bbLong = bb != null && !Double.isNaN(bb.upper[i]) && closes[i] > bb.upper[i];
      boolean bbShort = bb != null && !Double.isNaN(bb.lower[i]) && closes[i] < bb.lower[i];
      longCond = merge(c.requireAll, longCond, bbLong);
      shortCond = merge(c.requireAll, shortCond, bbShort);
    }
    if (c.enableEma) { any = true; longCond = merge(c.requireAll, longCond, emaFast[i] > emaSlow[i]); shortCond = merge(c.requireAll, shortCond, emaFast[i] < emaSlow[i]); }
    if (c.enableAo) { any = true; longCond = merge(c.requireAll, longCond, ao[i] > 0); shortCond = merge(c.requireAll, shortCond, ao[i] < 0); }
    if (c.enableSar) { any = true; longCond = merge(c.requireAll, longCond, closes[i] > sar.value[i]); shortCond = merge(c.requireAll, shortCond, closes[i] < sar.value[i]); }
    if (c.enableCci) { any = true; longCond = merge(c.requireAll, longCond, cciArr[i] > c.cciLong); shortCond = merge(c.requireAll, shortCond, cciArr[i] < c.cciShort); }
    if (c.enableAdx) { any = true; longCond = merge(c.requireAll, longCond, adx.adx[i] > c.adxThreshold && adx.plus[i] > adx.minus[i]); shortCond = merge(c.requireAll, shortCond, adx.adx[i] > c.adxThreshold && adx.minus[i] > adx.plus[i]); }
    if (c.enableTilson) {
      any = true;
      boolean tilsonLong = tilson != null && !Double.isNaN(tilson.value[i]) && !Double.isNaN(tilson.price[i]) && tilson.value[i] > tilson.price[i];
      boolean tilsonShort = tilson != null && !Double.isNaN(tilson.value[i]) && !Double.isNaN(tilson.price[i]) && tilson.value[i] < tilson.price[i];
      longCond = merge(c.requireAll, longCond, tilsonLong);
      shortCond = merge(c.requireAll, shortCond, tilsonShort);
    }
    if (c.enableSmi) {
      any = true;
      boolean smiReady = smi != null && !Double.isNaN(smi.value[i]) && !Double.isNaN(smi.signal[i]);
      boolean smiLong = smiReady && smi.value[i] > smi.signal[i];
      boolean smiShort = smiReady && smi.value[i] < smi.signal[i];
      if ("Zero Bias".equals(c.smiMode)) {
        smiLong = smiLong && smi.value[i] > 0.0;
        smiShort = smiShort && smi.value[i] < 0.0;
      }
      else if ("Guided Reversal".equals(c.smiMode)) {
        smiLong = smiReady && smi.crossAbove[i] && smi.value[i] < c.smiBottomGuide;
        smiShort = smiReady && smi.crossBelow[i] && smi.value[i] > c.smiTopGuide;

      }
      longCond = merge(c.requireAll, longCond, smiLong);
      shortCond = merge(c.requireAll, shortCond, smiShort);
    }
    return any ? new ForgeState(longCond, shortCond) : new ForgeState(false, false);
  }

  private static boolean merge(boolean requireAll, boolean current, boolean next) {
    return requireAll ? current && next : current || next;
  }

  private static final class OptimizerInputs {
    final DataContext ctx;
    final DataSeries s;
    final int n;
    final double[] closes;
    private double[] ao;
    private final Map<Integer, double[]> sma = new HashMap<>();
    private final Map<Integer, double[]> ema = new HashMap<>();
    private final Map<Integer, double[]> rsi = new HashMap<>();
    private final Map<Integer, double[]> cci = new HashMap<>();
    private final Map<Integer, double[]> atr = new HashMap<>();
    private final Map<Long, Macd> macd = new HashMap<>();
    private final Map<Long, Stoch> stoch = new HashMap<>();
    private final Map<String, Bands> bands = new HashMap<>();
    private final Map<String, Sar> sar = new HashMap<>();
    private final Map<Long, Adx> adx = new HashMap<>();
    private final Map<String, Super> superTrend = new HashMap<>();
    private final Map<String, Tilson> tilson = new HashMap<>();
    private final Map<String, Smi> smi = new HashMap<>();
    private final Map<String, HtfBias> htf = new HashMap<>();

    OptimizerInputs(DataContext ctx, DataSeries s) {
      this.ctx = ctx;
      this.s = s;
      this.n = s.size();
      this.closes = closeArray(s);
    }

    double[] sma(int len) {
      double[] out = sma.get(len);
      if (out == null) { out = MeridianIndicators.sma(closes, len); sma.put(len, out); }
      return out;
    }

    double[] ema(int len) {
      double[] out = ema.get(len);
      if (out == null) { out = MeridianIndicators.ema(closes, len); ema.put(len, out); }
      return out;
    }

    double[] rsi(int len) {
      double[] out = rsi.get(len);
      if (out == null) { out = MeridianIndicators.rsi(s, len); rsi.put(len, out); }
      return out;
    }

    Macd macd(int fast, int slow, int sig) {
      long key = key(fast, slow, sig);
      Macd out = macd.get(key);
      if (out == null) { out = MeridianIndicators.macd(s, fast, slow, sig); macd.put(key, out); }
      return out;
    }

    Stoch stoch(int kLen, int dLen, int smooth) {
      long key = key(kLen, dLen, smooth);
      Stoch out = stoch.get(key);
      if (out == null) { out = MeridianIndicators.stoch(s, kLen, dLen, smooth); stoch.put(key, out); }
      return out;
    }

    Bands bollinger(int len, double mult) {
      String key = len + "|" + mult;
      Bands out = bands.get(key);
      if (out == null) { out = MeridianIndicators.bollinger(closes, len, mult); bands.put(key, out); }
      return out;
    }

    double[] ao() {
      if (ao == null) ao = MeridianIndicators.ao(s);
      return ao;
    }

    Sar sar(double start, double inc, double max) {
      String key = start + "|" + inc + "|" + max;
      Sar out = sar.get(key);
      if (out == null) { out = MeridianIndicators.sar(s, start, inc, max); sar.put(key, out); }
      return out;
    }

    double[] cci(int len) {
      double[] out = cci.get(len);
      if (out == null) { out = MeridianIndicators.cci(s, len); cci.put(len, out); }
      return out;
    }

    Adx adx(int diLen, int adxLen) {
      long key = key(diLen, adxLen);
      Adx out = adx.get(key);
      if (out == null) { out = MeridianIndicators.adx(s, diLen, adxLen); adx.put(key, out); }
      return out;
    }

    Super superTrend(int len, double factor) {
      String key = len + "|" + factor;
      Super out = superTrend.get(key);
      if (out == null) { out = MeridianIndicators.superTrend(s, len, factor); superTrend.put(key, out); }
      return out;
    }

    Tilson tilson(String input, String method, int period) {
      String key = input + "|" + method + "|" + period;
      Tilson out = tilson.get(key);
      if (out == null) { out = MeridianIndicators.tilson(s, input, method, period); tilson.put(key, out); }
      return out;
    }

    Smi smi(String input, String method, int longPeriod, int shortPeriod, int signalPeriod) {
      String key = input + "|" + method + "|" + longPeriod + "|" + shortPeriod + "|" + signalPeriod;
      Smi out = smi.get(key);
      if (out == null) { out = MeridianIndicators.smi(s, input, method, longPeriod, shortPeriod, signalPeriod); smi.put(key, out); }
      return out;
    }

    double[] atr(int len) {
      double[] out = atr.get(len);
      if (out == null) { out = MeridianIndicators.atr(s, len); atr.put(len, out); }
      return out;
    }

    HtfBias htfBias(SettingsView cfg) {
      String key = cfg.useHtf + "|" + cfg.htfBarSize + "|" + cfg.htfEmaLen;
      HtfBias out = htf.get(key);
      if (out == null) { out = buildHtfBias(cfg, ctx, s, n); htf.put(key, out); }
      return out;
    }

    private static long key(int a, int b) {
      return (((long)a) << 32) ^ (b & 0xffffffffL);
    }

    private static long key(int a, int b, int c) {
      long h = 1469598103934665603L;
      h = (h ^ a) * 1099511628211L;
      h = (h ^ b) * 1099511628211L;
      return (h ^ c) * 1099511628211L;
    }
  }

  private static String parameterSummary(SettingsView cfg) {
    String filters = DashboardSupport.parameterSummaryFilters(cfg, true);
    return DashboardSupport.parameterSummaryCore(cfg) + " | " + DashboardSupport.parameterSummaryRisk(cfg) + (filters.isEmpty() ? "" : " | " + filters);
  }
}
