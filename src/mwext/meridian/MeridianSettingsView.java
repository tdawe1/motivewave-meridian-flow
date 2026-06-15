package mwext.meridian;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;

import java.awt.Color;

final class SettingsView {
  int swingLen, obLookback, maxOB;
  int smaFast, smaSlow, rsiLen, macdFast, macdSlow, macdSignal, stLen;
  int stochK, stochD, stochSmooth, bbLen, emaFast, emaSlow, cciLen, adxLen, diLen;
  int tilsonPeriod, smiLongPeriod, smiShortPeriod, smiSignalPeriod;
  int atrRiskLen, atrTrendLen, atrSmooth, htfEmaLen, obAlpha, dashboardLookback, optimizerLookback, optimizerMinTrades, projectionBars, optRefreshInterval;
  int dashboardXOffset, dashboardYOffset, dashboardScale;
  double rsiLong, rsiShort, stFactor, bbMult, sarStart, sarInc, sarMax, cciLong, cciShort, adxThreshold;
  double smiTopGuide, smiBottomGuide;
  double slMultEff, tpEff, tp1Eff, tp2Eff, tp3Eff, atrTrendMult;
  double slMultRaw, tpMultRaw, tp1MultRaw, tp2MultRaw, tp3MultRaw;
  boolean breakOnWick, showStruct, showBos, showOB, obMitWick, removeMitigated, obMean, obLabels;
  boolean useHtf, requireAll, enableSma, enableRsi, enableMacd, enableSt, enableStoch, enableBb, enableEma;
  boolean enableAo, enableSar, enableCci, enableAdx, enableTilson, enableSmi, showRisk, useBreakEven, showAtrTrend, showDashboard, showOptimizer, autoApplyOptimizer, showProjection, dashboardCompact, dashboardHideUnused, singleTarget, alertSl, alertTp, alertOb;
  String dashboardPosPreset;
  String signalMode, obFrom, signalSource, riskPreset, optimizerObjective, optimizerSearch, optRefreshMode, dashboardMode, tpMode;
  String tilsonInput, tilsonMethod, smiInput, smiMethod, smiMode;
  BarSize htfBarSize;
  String optimizerDepth;
  Color bullColor, bearColor, obBullColor, obBearColor, neutralColor;

  void read(com.motivewave.platform.sdk.common.Settings st, DataContext ctx) {
    swingLen = st.getInteger(MeridianFlowForge.SWING_LEN, 13);
    breakOnWick = "Wick".equals(st.getString(MeridianFlowForge.BREAK_SRC, "Close"));
    signalMode = st.getString(MeridianFlowForge.SIGNAL_MODE, "BOS + CHoCH");
    signalSource = st.getString(MeridianFlowForge.SIGNAL_SOURCE, "Structure + Forge");
    showStruct = st.getBoolean(MeridianFlowForge.SHOW_STRUCT, true);
    showBos = st.getBoolean(MeridianFlowForge.SHOW_BOS, true);
    showOB = st.getBoolean(MeridianFlowForge.SHOW_OB, true);
    obFrom = st.getString(MeridianFlowForge.OB_FROM, "BOS + CHoCH");
    obMitWick = "Wick".equals(st.getString(MeridianFlowForge.OB_MIT_SRC, "Wick"));
    removeMitigated = st.getBoolean(MeridianFlowForge.REMOVE_MIT, false);
    obLookback = st.getInteger(MeridianFlowForge.OB_LOOKBACK, 30);
    maxOB = st.getInteger(MeridianFlowForge.MAX_OB, 8);
    obAlpha = st.getInteger(MeridianFlowForge.OB_ALPHA, 38);
    obMean = st.getBoolean(MeridianFlowForge.OB_MEAN, false);
    obLabels = st.getBoolean(MeridianFlowForge.OB_LABELS, false);
    useHtf = st.getBoolean(MeridianFlowForge.USE_HTF, false);
    htfBarSize = st.getBarSize(MeridianFlowForge.HTF_BAR_SIZE);
    if (htfBarSize == null) htfBarSize = BarSize.minute(60);
    htfEmaLen = st.getInteger(MeridianFlowForge.HTF_EMA_LEN, 50);
    requireAll = st.getBoolean(MeridianFlowForge.REQUIRE_ALL, true);
    enableSma = st.getBoolean(MeridianFlowForge.ENABLE_SMA, true);
    smaFast = st.getInteger(MeridianFlowForge.SMA_FAST, 10);
    smaSlow = st.getInteger(MeridianFlowForge.SMA_SLOW, 20);
    enableRsi = st.getBoolean(MeridianFlowForge.ENABLE_RSI, false);
    rsiLen = st.getInteger(MeridianFlowForge.RSI_LEN, 14);
    rsiLong = st.getDouble(MeridianFlowForge.RSI_LONG, 50);
    rsiShort = st.getDouble(MeridianFlowForge.RSI_SHORT, 50);
    enableMacd = st.getBoolean(MeridianFlowForge.ENABLE_MACD, false);
    macdFast = st.getInteger(MeridianFlowForge.MACD_FAST, 12);
    macdSlow = st.getInteger(MeridianFlowForge.MACD_SLOW, 26);
    macdSignal = st.getInteger(MeridianFlowForge.MACD_SIGNAL, 9);
    enableSt = st.getBoolean(MeridianFlowForge.ENABLE_ST, false);
    stFactor = st.getDouble(MeridianFlowForge.ST_FACTOR, 3.0);
    stLen = st.getInteger(MeridianFlowForge.ST_LEN, 10);
    enableStoch = st.getBoolean(MeridianFlowForge.ENABLE_STOCH, false);
    stochK = st.getInteger(MeridianFlowForge.STOCH_K, 14);
    stochD = st.getInteger(MeridianFlowForge.STOCH_D, 3);
    stochSmooth = st.getInteger(MeridianFlowForge.STOCH_SMOOTH, 3);
    enableBb = st.getBoolean(MeridianFlowForge.ENABLE_BB, false);
    bbLen = st.getInteger(MeridianFlowForge.BB_LEN, 20);
    bbMult = st.getDouble(MeridianFlowForge.BB_MULT, 2.0);
    enableEma = st.getBoolean(MeridianFlowForge.ENABLE_EMA, false);
    emaFast = st.getInteger(MeridianFlowForge.EMA_FAST, 10);
    emaSlow = st.getInteger(MeridianFlowForge.EMA_SLOW, 20);
    enableAo = st.getBoolean(MeridianFlowForge.ENABLE_AO, false);
    enableSar = st.getBoolean(MeridianFlowForge.ENABLE_SAR, false);
    sarStart = st.getDouble(MeridianFlowForge.SAR_START, 0.02);
    sarInc = st.getDouble(MeridianFlowForge.SAR_INC, 0.02);
    sarMax = st.getDouble(MeridianFlowForge.SAR_MAX, 0.2);
    enableCci = st.getBoolean(MeridianFlowForge.ENABLE_CCI, false);
    cciLen = st.getInteger(MeridianFlowForge.CCI_LEN, 20);
    cciLong = st.getDouble(MeridianFlowForge.CCI_LONG, 0);
    cciShort = st.getDouble(MeridianFlowForge.CCI_SHORT, 0);
    enableAdx = st.getBoolean(MeridianFlowForge.ENABLE_ADX, false);
    adxLen = st.getInteger(MeridianFlowForge.ADX_LEN, 14);
    diLen = st.getInteger(MeridianFlowForge.DI_LEN, 14);
    adxThreshold = st.getDouble(MeridianFlowForge.ADX_THRESHOLD, 20);
    enableTilson = st.getBoolean(MeridianFlowForge.ENABLE_TILSON, false);
    tilsonInput = st.getString(MeridianFlowForge.TILSON_INPUT, "High");
    tilsonMethod = st.getString(MeridianFlowForge.TILSON_METHOD, "MEMA");
    tilsonPeriod = st.getInteger(MeridianFlowForge.TILSON_PERIOD, 12);
    enableSmi = st.getBoolean(MeridianFlowForge.ENABLE_SMI, false);
    smiInput = st.getString(MeridianFlowForge.SMI_INPUT, "Open");
    smiMethod = st.getString(MeridianFlowForge.SMI_METHOD, "SMA");
    smiLongPeriod = st.getInteger(MeridianFlowForge.SMI_LONG_PERIOD, 10);
    smiShortPeriod = st.getInteger(MeridianFlowForge.SMI_SHORT_PERIOD, 12);
    smiSignalPeriod = st.getInteger(MeridianFlowForge.SMI_SIGNAL_PERIOD, 6);
    smiTopGuide = st.getDouble(MeridianFlowForge.SMI_TOP_GUIDE, 0.1);
    smiBottomGuide = st.getDouble(MeridianFlowForge.SMI_BOTTOM_GUIDE, -0.1);
    smiMode = st.getString(MeridianFlowForge.SMI_MODE, "Line vs Signal");
    atrRiskLen = st.getInteger(MeridianFlowForge.ATR_RISK_LEN, 13);
    slMultRaw = st.getDouble(MeridianFlowForge.SL_MULT, 1.5);
    tpMultRaw = st.getDouble(MeridianFlowForge.TP_MULT, 2.0);
    tp1MultRaw = st.getDouble(MeridianFlowForge.TP1_MULT, 1.0);
    tp2MultRaw = st.getDouble(MeridianFlowForge.TP2_MULT, 2.0);
    tp3MultRaw = st.getDouble(MeridianFlowForge.TP3_MULT, 3.0);
    riskPreset = st.getString(MeridianFlowForge.RISK_PRESET, "Balanced");
    tpMode = st.getString(MeridianFlowForge.TP_MODE, "Three Targets");
    singleTarget = "Single Target".equals(tpMode);

    switch (riskPreset) {
      case "Conservative" -> { slMultEff = 2.5; tpEff = 2.0; tp1Eff = 1.0; tp2Eff = 2.0; tp3Eff = 4.0; }
      case "Aggressive" -> { slMultEff = 1.0; tpEff = 2.5; tp1Eff = 1.5; tp2Eff = 2.5; tp3Eff = 4.0; }
      case "Scalping" -> { slMultEff = 0.8; tpEff = 1.5; tp1Eff = 0.8; tp2Eff = 1.5; tp3Eff = 2.0; }
      case "Custom" -> { slMultEff = slMultRaw; tpEff = tpMultRaw; tp1Eff = tp1MultRaw; tp2Eff = tp2MultRaw; tp3Eff = tp3MultRaw; }
      default -> { slMultEff = 1.5; tpEff = 2.0; tp1Eff = 1.0; tp2Eff = 2.0; tp3Eff = 3.0; }
    }
    if (!singleTarget) {
      if (tp1Eff >= tp2Eff) tp2Eff = tp1Eff + 0.5;
      if (tp2Eff >= tp3Eff) tp3Eff = tp2Eff + 0.5;
    }
    showRisk = st.getBoolean(MeridianFlowForge.SHOW_RISK, true);
    useBreakEven = st.getBoolean(MeridianFlowForge.USE_BE, true);
    showAtrTrend = st.getBoolean(MeridianFlowForge.SHOW_ATR_TREND, true);
    showDashboard = st.getBoolean(MeridianFlowForge.SHOW_DASHBOARD, true);
    dashboardMode = st.getString(MeridianFlowForge.DASHBOARD_MODE, "Full");
    dashboardCompact = "Compact".equals(dashboardMode);
    dashboardHideUnused = st.getBoolean(MeridianFlowForge.DASHBOARD_HIDE_UNUSED, false);
    dashboardPosPreset = st.getString(MeridianFlowForge.DASHBOARD_POS_PRESET, "Top Left");
    dashboardXOffset = st.getInteger(MeridianFlowForge.DASHBOARD_X_OFFSET, 12);
    dashboardYOffset = st.getInteger(MeridianFlowForge.DASHBOARD_Y_OFFSET, 12);
    dashboardScale = st.getInteger(MeridianFlowForge.DASHBOARD_SCALE, 100);
    showProjection = st.getBoolean(MeridianFlowForge.SHOW_PROJECTION, true);
    projectionBars = st.getInteger(MeridianFlowForge.PROJECTION_BARS, 16);
    showOptimizer = st.getBoolean(MeridianFlowForge.SHOW_OPTIMIZER, false);
    autoApplyOptimizer = st.getBoolean(MeridianFlowForge.AUTO_APPLY_OPTIMIZER, false);
    optimizerLookback = st.getInteger(MeridianFlowForge.OPT_LOOKBACK, 2500);
    optimizerMinTrades = st.getInteger(MeridianFlowForge.OPT_MIN_TRADES, 8);
    optimizerObjective = st.getString(MeridianFlowForge.OPT_OBJECTIVE, "Balanced");
    optimizerSearch = st.getString(MeridianFlowForge.OPT_SEARCH, "NQ 5/15m Fast");
    optRefreshMode = st.getString(MeridianFlowForge.OPT_REFRESH_MODE, "On Demand");
    optimizerDepth = st.getString(MeridianFlowForge.OPT_DEPTH, "Fast");
    optRefreshInterval = st.getInteger(MeridianFlowForge.OPT_REFRESH_INTERVAL, 20);
    dashboardLookback = st.getInteger(MeridianFlowForge.DASHBOARD_LOOKBACK, 5000);
    atrTrendLen = st.getInteger(MeridianFlowForge.ATR_TREND_LEN, 10);
    atrTrendMult = st.getDouble(MeridianFlowForge.ATR_TREND_MULT, 5.0);
    atrSmooth = st.getInteger(MeridianFlowForge.ATR_SMOOTH, 5);
    Defaults d = ctx.getDefaults();
    bullColor = st.getColor(MeridianFlowForge.BULL_COLOR, d == null ? new Color(0, 230, 118) : d.getGreen());
    bearColor = st.getColor(MeridianFlowForge.BEAR_COLOR, d == null ? new Color(255, 82, 82) : d.getRed());
    obBullColor = st.getColor(MeridianFlowForge.OB_BULL_COLOR, new Color(38, 166, 154));
    obBearColor = st.getColor(MeridianFlowForge.OB_BEAR_COLOR, new Color(239, 83, 80));
    neutralColor = new Color(245, 180, 40);
    alertSl = st.getBoolean(MeridianFlowForge.ALERT_SL, true);
    alertTp = st.getBoolean(MeridianFlowForge.ALERT_TP, false);
    alertOb = st.getBoolean(MeridianFlowForge.ALERT_OB, false);

  }


  SettingsView copy() {
    SettingsView c = new SettingsView();
    c.swingLen = swingLen; c.obLookback = obLookback; c.maxOB = maxOB;
    c.smaFast = smaFast; c.smaSlow = smaSlow; c.rsiLen = rsiLen; c.macdFast = macdFast; c.macdSlow = macdSlow; c.macdSignal = macdSignal; c.stLen = stLen;
    c.stochK = stochK; c.stochD = stochD; c.stochSmooth = stochSmooth; c.bbLen = bbLen; c.emaFast = emaFast; c.emaSlow = emaSlow; c.cciLen = cciLen; c.adxLen = adxLen; c.diLen = diLen;
    c.tilsonPeriod = tilsonPeriod; c.smiLongPeriod = smiLongPeriod; c.smiShortPeriod = smiShortPeriod; c.smiSignalPeriod = smiSignalPeriod;
    c.atrRiskLen = atrRiskLen; c.atrTrendLen = atrTrendLen; c.atrSmooth = atrSmooth; c.htfEmaLen = htfEmaLen; c.obAlpha = obAlpha; c.dashboardLookback = dashboardLookback;
    c.optimizerLookback = optimizerLookback; c.optimizerMinTrades = optimizerMinTrades; c.projectionBars = projectionBars; c.optRefreshInterval = optRefreshInterval;
    c.dashboardXOffset = dashboardXOffset; c.dashboardYOffset = dashboardYOffset; c.dashboardScale = dashboardScale;
    c.rsiLong = rsiLong; c.rsiShort = rsiShort; c.stFactor = stFactor; c.bbMult = bbMult; c.sarStart = sarStart; c.sarInc = sarInc; c.sarMax = sarMax; c.cciLong = cciLong; c.cciShort = cciShort; c.adxThreshold = adxThreshold;
    c.smiTopGuide = smiTopGuide; c.smiBottomGuide = smiBottomGuide;
    c.slMultEff = slMultEff; c.tpEff = tpEff; c.tp1Eff = tp1Eff; c.tp2Eff = tp2Eff; c.tp3Eff = tp3Eff; c.atrTrendMult = atrTrendMult;
    c.slMultRaw = slMultRaw; c.tpMultRaw = tpMultRaw; c.tp1MultRaw = tp1MultRaw; c.tp2MultRaw = tp2MultRaw; c.tp3MultRaw = tp3MultRaw;
    c.breakOnWick = breakOnWick; c.showStruct = showStruct; c.showBos = showBos; c.showOB = showOB; c.obMitWick = obMitWick; c.removeMitigated = removeMitigated; c.obMean = obMean; c.obLabels = obLabels;
    c.useHtf = useHtf; c.requireAll = requireAll; c.enableSma = enableSma; c.enableRsi = enableRsi; c.enableMacd = enableMacd; c.enableSt = enableSt; c.enableStoch = enableStoch; c.enableBb = enableBb; c.enableEma = enableEma;
    c.enableAo = enableAo; c.enableSar = enableSar; c.enableCci = enableCci; c.enableAdx = enableAdx; c.enableTilson = enableTilson; c.enableSmi = enableSmi; c.showRisk = showRisk; c.useBreakEven = useBreakEven; c.showAtrTrend = showAtrTrend; c.showDashboard = showDashboard; c.showOptimizer = showOptimizer; c.autoApplyOptimizer = autoApplyOptimizer; c.showProjection = showProjection; c.dashboardCompact = dashboardCompact; c.dashboardHideUnused = dashboardHideUnused; c.singleTarget = singleTarget;
    c.alertSl = alertSl; c.alertTp = alertTp; c.alertOb = alertOb;
    c.signalMode = signalMode; c.obFrom = obFrom; c.signalSource = signalSource; c.riskPreset = riskPreset; c.optimizerObjective = optimizerObjective; c.optimizerSearch = optimizerSearch; c.optRefreshMode = optRefreshMode; c.optimizerDepth = optimizerDepth; c.dashboardMode = dashboardMode; c.tpMode = tpMode;
    c.dashboardPosPreset = dashboardPosPreset;
    c.tilsonInput = tilsonInput; c.tilsonMethod = tilsonMethod; c.smiInput = smiInput; c.smiMethod = smiMethod; c.smiMode = smiMode;
    c.htfBarSize = htfBarSize;
    c.bullColor = bullColor; c.bearColor = bearColor; c.obBullColor = obBullColor; c.obBearColor = obBearColor; c.neutralColor = neutralColor;
    return c;
  }

}
