package mwext.meridian;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.Defaults;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

final class SettingsView {
  private static final Field[] COPY_FIELDS = copyFields();

  int swingLen, obLookback, maxOB;
  int smaFast, smaSlow, rsiLen, macdFast, macdSlow, macdSignal, stLen;
  int stochK, stochD, stochSmooth, bbLen, emaFast, emaSlow, cciLen, adxLen, diLen;
  int tilsonPeriod, smiLongPeriod, smiShortPeriod, smiSignalPeriod;
  int atrRiskLen, atrTrendLen, atrSmooth, htfEmaLen, obAlpha, dashboardLookback, optimizerLookback, optimizerMinTrades, projectionBars, optRefreshInterval;
  int dashboardXOffset, dashboardYOffset, dashboardScale, signalImageSize, signalImageOffset;
  int tradingBotAmplitude, tradingBotBbPeriod, tradingBotAtrLen, tradingBotRsiLen, tradingBotSwing;
  int tradingBotTsiFastLong, tradingBotTsiFastShort, tradingBotTsiFastSignal, tradingBotTsiSlowLong, tradingBotTsiSlowShort, tradingBotTsiSlowSignal;
  double rsiLong, rsiShort, stFactor, bbMult, sarStart, sarInc, sarMax, cciLong, cciShort, adxThreshold;
  double smiTopGuide, smiBottomGuide;
  double slMultEff, tpEff, tp1Eff, tp2Eff, tp3Eff, atrTrendMult;
  double slMultRaw, tpMultRaw, tp1MultRaw, tp2MultRaw, tp3MultRaw;
  double tradingBotChannelDeviation, tradingBotBbDeviation, tradingBotRsiTop, tradingBotRsiBot;
  boolean breakOnWick, showStruct, showBos, showOB, obMitWick, removeMitigated, obMean, obLabels;
  boolean useHtf, requireAll, enableSma, enableRsi, enableMacd, enableSt, enableStoch, enableBb, enableEma;
  boolean enableAo, enableSar, enableCci, enableAdx, enableTilson, enableSmi, showRisk, useBreakEven, showAtrTrend, showDashboard, showOptimizer, autoApplyOptimizer, showProjection, dashboardCompact, dashboardHideUnused, singleTarget, alertSl, alertTp, alertOb;
  boolean showSignalImage;
  boolean enableTradingBot, tradingBotLongSide, tradingBotShortSide, tradingBotUseHalfTrend, tradingBotUseTrendLine, tradingBotUseTsiCurl, tradingBotUseAdema;
  String dashboardPosPreset;
  String signalMode, obFrom, signalSource, riskPreset, optimizerObjective, optimizerSearch, optRefreshMode, dashboardMode, tpMode;
  String tilsonInput, tilsonMethod, smiInput, smiMethod, smiMode;
  String tradingBotFilter, tradingBotTsiSpeed;
  BarSize htfBarSize;
  File signalImageFile;
  String optimizerDepth;
  Color bullColor, bearColor, obBullColor, obBearColor, neutralColor;

  void read(com.motivewave.platform.sdk.common.Settings st, DataContext ctx) {
    swingLen = st.getInteger(MeridianFlowForge.SWING_LEN, 5);
    breakOnWick = MeridianOptions.WICK.equals(st.getString(MeridianFlowForge.BREAK_SRC, MeridianOptions.WICK));
    signalMode = st.getString(MeridianFlowForge.SIGNAL_MODE, SignalModeOption.BOS_CHOCH.label);
    signalSource = st.getString(MeridianFlowForge.SIGNAL_SOURCE, SignalSourceOption.STRUCTURE_FORGE.label);
    showStruct = st.getBoolean(MeridianFlowForge.SHOW_STRUCT, true);
    showBos = st.getBoolean(MeridianFlowForge.SHOW_BOS, true);
    showOB = st.getBoolean(MeridianFlowForge.SHOW_OB, true);
    obFrom = st.getString(MeridianFlowForge.OB_FROM, SignalModeOption.BOS_CHOCH.label);
    obMitWick = MeridianOptions.WICK.equals(st.getString(MeridianFlowForge.OB_MIT_SRC, MeridianOptions.WICK));
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
    tilsonInput = st.getString(MeridianFlowForge.TILSON_INPUT, MeridianOptions.HIGH);
    tilsonMethod = st.getString(MeridianFlowForge.TILSON_METHOD, MeridianOptions.MEMA);
    tilsonPeriod = st.getInteger(MeridianFlowForge.TILSON_PERIOD, 12);
    enableSmi = st.getBoolean(MeridianFlowForge.ENABLE_SMI, false);
    smiInput = st.getString(MeridianFlowForge.SMI_INPUT, MeridianOptions.OPEN);
    smiMethod = st.getString(MeridianFlowForge.SMI_METHOD, MeridianOptions.SMA);
    smiLongPeriod = st.getInteger(MeridianFlowForge.SMI_LONG_PERIOD, 10);
    smiShortPeriod = st.getInteger(MeridianFlowForge.SMI_SHORT_PERIOD, 12);
    smiSignalPeriod = st.getInteger(MeridianFlowForge.SMI_SIGNAL_PERIOD, 6);
    smiTopGuide = st.getDouble(MeridianFlowForge.SMI_TOP_GUIDE, 0.1);
    smiBottomGuide = st.getDouble(MeridianFlowForge.SMI_BOTTOM_GUIDE, -0.1);
    smiMode = st.getString(MeridianFlowForge.SMI_MODE, SmiModeOption.LINE_VS_SIGNAL.label);
    enableTradingBot = st.getBoolean(MeridianFlowForge.ENABLE_TRADING_BOT, false);
    tradingBotLongSide = st.getBoolean(MeridianFlowForge.TB_LONG_SIDE, true);
    tradingBotShortSide = st.getBoolean(MeridianFlowForge.TB_SHORT_SIDE, true);
    tradingBotUseHalfTrend = st.getBoolean(MeridianFlowForge.TB_USE_HALF_TREND, true);
    tradingBotUseTrendLine = st.getBoolean(MeridianFlowForge.TB_USE_TRENDLINE, true);
    tradingBotUseTsiCurl = st.getBoolean(MeridianFlowForge.TB_USE_TSI_CURL, false);
    tradingBotUseAdema = st.getBoolean(MeridianFlowForge.TB_USE_ADEMA, true);
    tradingBotAmplitude = st.getInteger(MeridianFlowForge.TB_AMPLITUDE, 45);
    tradingBotChannelDeviation = st.getDouble(MeridianFlowForge.TB_CHANNEL_DEVIATION, 2.0);
    tradingBotBbPeriod = st.getInteger(MeridianFlowForge.TB_BB_PERIOD, 100);
    tradingBotBbDeviation = st.getDouble(MeridianFlowForge.TB_BB_DEVIATION, 1.5);
    tradingBotAtrLen = st.getInteger(MeridianFlowForge.TB_ATR_LEN, 5);
    tradingBotRsiLen = st.getInteger(MeridianFlowForge.TB_RSI_LEN, 7);
    tradingBotRsiTop = st.getDouble(MeridianFlowForge.TB_RSI_TOP, 45.0);
    tradingBotRsiBot = st.getDouble(MeridianFlowForge.TB_RSI_BOT, 10.0);
    tradingBotFilter = st.getString(MeridianFlowForge.TB_FILTER, MeridianOptions.NO_FILTERING);
    tradingBotSwing = st.getInteger(MeridianFlowForge.TB_SWING, 5);
    tradingBotTsiSpeed = st.getString(MeridianFlowForge.TB_TSI_SPEED, MeridianOptions.FAST);
    tradingBotTsiFastLong = st.getInteger(MeridianFlowForge.TB_TSI_FAST_LONG, 25);
    tradingBotTsiFastShort = st.getInteger(MeridianFlowForge.TB_TSI_FAST_SHORT, 5);
    tradingBotTsiFastSignal = st.getInteger(MeridianFlowForge.TB_TSI_FAST_SIGNAL, 14);
    tradingBotTsiSlowLong = st.getInteger(MeridianFlowForge.TB_TSI_SLOW_LONG, 25);
    tradingBotTsiSlowShort = st.getInteger(MeridianFlowForge.TB_TSI_SLOW_SHORT, 13);
    tradingBotTsiSlowSignal = st.getInteger(MeridianFlowForge.TB_TSI_SLOW_SIGNAL, 13);
    atrRiskLen = st.getInteger(MeridianFlowForge.ATR_RISK_LEN, 13);
    slMultRaw = st.getDouble(MeridianFlowForge.SL_MULT, 1.5);
    tpMultRaw = st.getDouble(MeridianFlowForge.TP_MULT, 2.0);
    tp1MultRaw = st.getDouble(MeridianFlowForge.TP1_MULT, 1.0);
    tp2MultRaw = st.getDouble(MeridianFlowForge.TP2_MULT, 2.0);
    tp3MultRaw = st.getDouble(MeridianFlowForge.TP3_MULT, 3.0);
    riskPreset = st.getString(MeridianFlowForge.RISK_PRESET, MeridianOptions.BALANCED);
    tpMode = st.getString(MeridianFlowForge.TP_MODE, TakeProfitModeOption.THREE.label);
    singleTarget = TakeProfitModeOption.isSingle(tpMode);

    RiskPresetOption.from(riskPreset).apply(this);
    normalizeTargets();
    showRisk = st.getBoolean(MeridianFlowForge.SHOW_RISK, true);
    useBreakEven = st.getBoolean(MeridianFlowForge.USE_BE, true);
    showAtrTrend = st.getBoolean(MeridianFlowForge.SHOW_ATR_TREND, true);
    showDashboard = st.getBoolean(MeridianFlowForge.SHOW_DASHBOARD, true);
    dashboardMode = st.getString(MeridianFlowForge.DASHBOARD_MODE, MeridianOptions.FULL);
    dashboardCompact = MeridianOptions.COMPACT.equals(dashboardMode);
    dashboardHideUnused = st.getBoolean(MeridianFlowForge.DASHBOARD_HIDE_UNUSED, false);
    dashboardPosPreset = st.getString(MeridianFlowForge.DASHBOARD_POS_PRESET, "Top Left");
    dashboardXOffset = st.getInteger(MeridianFlowForge.DASHBOARD_X_OFFSET, 12);
    dashboardYOffset = st.getInteger(MeridianFlowForge.DASHBOARD_Y_OFFSET, 12);
    dashboardScale = st.getInteger(MeridianFlowForge.DASHBOARD_SCALE, 100);
    showProjection = st.getBoolean(MeridianFlowForge.SHOW_PROJECTION, true);
    projectionBars = st.getInteger(MeridianFlowForge.PROJECTION_BARS, 16);
    showSignalImage = st.getBoolean(MeridianFlowForge.SHOW_SIGNAL_IMAGE, true);
    signalImageFile = st.getFile(MeridianFlowForge.SIGNAL_IMAGE_FILE);
    signalImageSize = st.getInteger(MeridianFlowForge.SIGNAL_IMAGE_SIZE, 72);
    signalImageOffset = st.getInteger(MeridianFlowForge.SIGNAL_IMAGE_OFFSET, 8);
    showOptimizer = st.getBoolean(MeridianFlowForge.SHOW_OPTIMIZER, false);
    autoApplyOptimizer = st.getBoolean(MeridianFlowForge.AUTO_APPLY_OPTIMIZER, false);
    optimizerLookback = st.getInteger(MeridianFlowForge.OPT_LOOKBACK, 800);
    optimizerMinTrades = st.getInteger(MeridianFlowForge.OPT_MIN_TRADES, 8);
    optimizerObjective = st.getString(MeridianFlowForge.OPT_OBJECTIVE, MeridianOptions.BALANCED);
    optimizerSearch = st.getString(MeridianFlowForge.OPT_SEARCH, MeridianOptions.NQ_5_15M_FAST);
    optRefreshMode = st.getString(MeridianFlowForge.OPT_REFRESH_MODE, OptimizerRefreshOption.EVERY_N_BARS.label);
    optimizerDepth = st.getString(MeridianFlowForge.OPT_DEPTH, OptimizerDepthOption.FAST.label);
    optRefreshInterval = st.getInteger(MeridianFlowForge.OPT_REFRESH_INTERVAL, 20);
    dashboardLookback = st.getInteger(MeridianFlowForge.DASHBOARD_LOOKBACK, 800);
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
    try {
      for (Field field : COPY_FIELDS) {
        field.set(c, field.get(this));
      }
    }
    catch (IllegalAccessException e) {
      throw new IllegalStateException("Unable to copy settings", e);
    }
    return c;
  }

  private static Field[] copyFields() {
    List<Field> fields = new ArrayList<>();
    for (Field field : SettingsView.class.getDeclaredFields()) {
      if (!Modifier.isStatic(field.getModifiers())) fields.add(field);
    }
    return fields.toArray(new Field[0]);
  }

  String tuningKey() {
    StringBuilder b = new StringBuilder(512);
    appendTuningSettings(b);
    return b.toString();
  }

  void appendOptimizerSettings(StringBuilder b) {
    b.append('|').append(optimizerLookback).append('|').append(optimizerMinTrades).append('|').append(optimizerObjective).append('|').append(optimizerSearch).append('|').append(optimizerDepth);
    appendTuningSettings(b);
  }

  void appendTuningSettings(StringBuilder b) {
    b.append('|').append(swingLen).append('|').append(breakOnWick).append('|').append(signalMode).append('|').append(signalSource);
    b.append('|').append(useHtf).append('|').append(htfBarSize).append('|').append(htfEmaLen).append('|').append(requireAll);
    b.append('|').append(enableSma).append('|').append(smaFast).append('|').append(smaSlow);
    b.append('|').append(enableRsi).append('|').append(rsiLen).append('|').append(rsiLong).append('|').append(rsiShort);
    b.append('|').append(enableMacd).append('|').append(macdFast).append('|').append(macdSlow).append('|').append(macdSignal);
    b.append('|').append(enableSt).append('|').append(stLen).append('|').append(stFactor);
    b.append('|').append(enableStoch).append('|').append(stochK).append('|').append(stochD).append('|').append(stochSmooth);
    b.append('|').append(enableBb).append('|').append(bbLen).append('|').append(bbMult);
    b.append('|').append(enableEma).append('|').append(emaFast).append('|').append(emaSlow);
    b.append('|').append(enableAo).append('|').append(enableSar).append('|').append(sarStart).append('|').append(sarInc).append('|').append(sarMax);
    b.append('|').append(enableCci).append('|').append(cciLen).append('|').append(cciLong).append('|').append(cciShort);
    b.append('|').append(enableAdx).append('|').append(diLen).append('|').append(adxLen).append('|').append(adxThreshold);
    b.append('|').append(enableTilson).append('|').append(tilsonInput).append('|').append(tilsonMethod).append('|').append(tilsonPeriod);
    b.append('|').append(enableSmi).append('|').append(smiInput).append('|').append(smiMethod).append('|').append(smiLongPeriod).append('|').append(smiShortPeriod).append('|').append(smiSignalPeriod)
      .append('|').append(smiTopGuide).append('|').append(smiBottomGuide).append('|').append(smiMode);
    b.append('|').append(atrRiskLen).append('|').append(slMultEff).append('|').append(tpEff).append('|').append(tp1Eff).append('|').append(tp2Eff).append('|').append(tp3Eff).append('|').append(riskPreset).append('|').append(tpMode).append('|').append(singleTarget).append('|').append(useBreakEven);
  }

  void normalizeTargets() {
    if (!singleTarget) {
      if (tp1Eff >= tp2Eff) tp2Eff = tp1Eff + 0.5;
      if (tp2Eff >= tp3Eff) tp3Eff = tp2Eff + 0.5;
    }
  }

}
