package mwext.meridian;
import static mwext.meridian.MeridianIndicators.*;
import static mwext.meridian.DashboardSupport.*;

import com.motivewave.platform.sdk.common.BarSize;
import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.common.Coordinate;
import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Enums$MarkerType;
import com.motivewave.platform.sdk.common.Enums$Position;
import com.motivewave.platform.sdk.common.Enums$Size;
import com.motivewave.platform.sdk.common.Inputs;
import com.motivewave.platform.sdk.common.MarkerInfo;
import com.motivewave.platform.sdk.common.NVP;
import com.motivewave.platform.sdk.common.PathInfo;
import com.motivewave.platform.sdk.common.Util;
import com.motivewave.platform.sdk.common.desc.BarSizeDescriptor;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.ColorDescriptor;
import com.motivewave.platform.sdk.common.desc.DiscreteDescriptor;
import com.motivewave.platform.sdk.common.desc.DoubleDescriptor;
import com.motivewave.platform.sdk.common.desc.IndicatorDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.MarkerDescriptor;
import com.motivewave.platform.sdk.common.desc.PathDescriptor;
import com.motivewave.platform.sdk.common.desc.SettingGroup;
import com.motivewave.platform.sdk.common.desc.SettingTab;
import com.motivewave.platform.sdk.common.desc.SettingsDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.draw.Box;
import com.motivewave.platform.sdk.draw.Figure;
import com.motivewave.platform.sdk.draw.Label;
import com.motivewave.platform.sdk.draw.Line;
import com.motivewave.platform.sdk.draw.Marker;
import com.motivewave.platform.sdk.study.RuntimeDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@StudyHeader(
  namespace = "codex.meridian",
  id = "MERIDIAN_FLOW_FORGE",
  name = "Meridian Flow Forge",
  label = "Meridian Forge",
  desc =  "Meridian Flow Forge is a comprehensive trading study that identifies market structure, order blocks, and provides trade signals based on various technical indicators. It includes features for risk management, optimization, and visual representation of key levels.",
  menu = "tdawe",
  overlay = true,
  signals = true,
  studyOverlay = true,
  requiresBarUpdates = false
)
public class MeridianFlowForge extends Study
{
  static final String VERSION = "v13-deep-optimizer-dashboard-fix";
  private static volatile boolean loggedCalculate;

  private final MeridianOptimizer optimizer = new MeridianOptimizer();
  private volatile String calculationCacheKey = "";
  private volatile DashboardFigure lastDashboardFigure;
  private volatile SettingsView lastDashboardCfg;
  private volatile int lastDashboardSignalIndex = -1;

  static final String SWING_LEN = "swingLen";
  static final String BREAK_SRC = "breakSrc";
  static final String SIGNAL_MODE = "signalMode";
  static final String SIGNAL_SOURCE = "signalSource";
  static final String SHOW_STRUCT = "showStruct";
  static final String SHOW_BOS = "showBos";

  static final String SHOW_OB = "showOB";
  static final String OB_FROM = "obFrom";
  static final String OB_MIT_SRC = "obMitSrc";
  static final String REMOVE_MIT = "removeMitigated";
  static final String OB_LOOKBACK = "obLookback";
  static final String MAX_OB = "maxOB";
  static final String OB_ALPHA = "obAlpha";
  static final String OB_MEAN = "obMean";
  static final String OB_LABELS = "obLabels";

  static final String USE_HTF = "useHtf";
  static final String HTF_BAR_SIZE = "htfBarSize";
  static final String HTF_EMA_LEN = "htfEmaLen";

  static final String REQUIRE_ALL = "requireAll";
  static final String SIGNAL_GROUP = "signalGroup";
  static final String ENABLE_SMA = "enableSma";
  static final String SMA_FAST = "smaFast";
  static final String SMA_SLOW = "smaSlow";
  static final String ENABLE_RSI = "enableRsi";
  static final String RSI_LEN = "rsiLen";
  static final String RSI_LONG = "rsiLong";
  static final String RSI_SHORT = "rsiShort";
  static final String ENABLE_MACD = "enableMacd";
  static final String MACD_FAST = "macdFast";
  static final String MACD_SLOW = "macdSlow";
  static final String MACD_SIGNAL = "macdSignal";
  static final String ENABLE_ST = "enableSt";
  static final String ST_FACTOR = "stFactor";
  static final String ST_LEN = "stLen";
  static final String ENABLE_STOCH = "enableStoch";
  static final String STOCH_K = "stochK";
  static final String STOCH_D = "stochD";
  static final String STOCH_SMOOTH = "stochSmooth";
  static final String ENABLE_BB = "enableBb";
  static final String BB_LEN = "bbLen";
  static final String BB_MULT = "bbMult";
  static final String ENABLE_EMA = "enableEma";
  static final String EMA_FAST = "emaFast";
  static final String EMA_SLOW = "emaSlow";
  static final String ENABLE_AO = "enableAo";
  static final String ENABLE_SAR = "enableSar";
  static final String SAR_START = "sarStart";
  static final String SAR_INC = "sarInc";
  static final String SAR_MAX = "sarMax";
  static final String ENABLE_CCI = "enableCci";
  static final String CCI_LEN = "cciLen";
  static final String CCI_LONG = "cciLong";
  static final String CCI_SHORT = "cciShort";
  static final String ENABLE_ADX = "enableAdx";
  static final String ENABLE_TILSON = "enableTilson";
  static final String TILSON_INPUT = "tilsonInput";
  static final String TILSON_METHOD = "tilsonMethod";
  static final String TILSON_PERIOD = "tilsonPeriod";
  static final String ENABLE_SMI = "enableSmi";
  static final String SMI_INPUT = "smiInput";
  static final String SMI_METHOD = "smiMethod";
  static final String SMI_LONG_PERIOD = "smiLongPeriod";
  static final String SMI_SHORT_PERIOD = "smiShortPeriod";
  static final String SMI_SIGNAL_PERIOD = "smiSignalPeriod";
  static final String SMI_TOP_GUIDE = "smiTopGuide";
  static final String SMI_BOTTOM_GUIDE = "smiBottomGuide";
  static final String SMI_MODE = "smiMode";
  static final String ADX_LEN = "adxLen";
  static final String DI_LEN = "diLen";
  static final String ADX_THRESHOLD = "adxThreshold";

  static final String RISK_PRESET = "riskPreset";
  static final String SHOW_OPTIMIZER = "showOptimizer";
  static final String OPT_LOOKBACK = "optimizerLookback";
  static final String OPT_MIN_TRADES = "optimizerMinTrades";
  static final String OPT_OBJECTIVE = "optimizerObjective";
  static final String OPT_SEARCH = "optimizerSearch";
  static final String OPT_REFRESH_MODE = "optimizerRefreshMode";
  static final String OPT_REFRESH_INTERVAL = "optimizerRefreshInterval";
  static final String OPT_DEPTH = "optimizerDepth";
  static final String APPLY_OPTIMIZER = "applyOptimizer";
  static final String DASHBOARD_MODE = "dashboardMode";
  static final String DASHBOARD_HIDE_UNUSED = "dashboardHideUnused";
  static final String SHOW_DASHBOARD = "showDashboard";
  static final String DASHBOARD_LOOKBACK = "dashboardLookback";
  static final String SHOW_PROJECTION = "showProjection";
  static final String PROJECTION_BARS = "projectionBars";
  static final String DASHBOARD_POS_PRESET = "dashboardPosPreset";
  static final String DASHBOARD_X_OFFSET = "dashboardXOffset";
  static final String DASHBOARD_Y_OFFSET = "dashboardYOffset";
  static final String DASHBOARD_SCALE = "dashboardScale";

  static final String ATR_RISK_LEN = "atrRiskLen";
  static final String SL_MULT = "slMult";
  static final String TP_MODE = "tpMode";
  static final String TP_MULT = "tpMult";
  static final String TP1_MULT = "tp1Mult";
  static final String TP2_MULT = "tp2Mult";
  static final String TP3_MULT = "tp3Mult";
  static final String SHOW_RISK = "showRisk";
  static final String USE_BE = "useBreakEven";

  static final String SHOW_ATR_TREND = "showAtrTrend";
  static final String ATR_TREND_LEN = "atrTrendLen";
  static final String ATR_TREND_MULT = "atrTrendMult";
  static final String ATR_SMOOTH = "atrSmooth";
  static final String ATR_TREND_PATH = "atrTrendPath";

  static final String BULL_COLOR = "bullColor";
  static final String BEAR_COLOR = "bearColor";
  static final String OB_BULL_COLOR = "obBullColor";
  static final String OB_BEAR_COLOR = "obBearColor";
  static final String UP_MARKER = "upMarker";
  static final String DOWN_MARKER = "downMarker";
  static final String STRUCT_PATH = "structPath";
  static final String RISK_PATH = "riskPath";

  static final String ALERT_SL = "alertSl";
  static final String ALERT_TP = "alertTp";
  static final String ALERT_OB = "alertOb";

  private static final int LINE_FORWARD_BARS = 20;
  private static final int LINE_UPDATE_BARS = 5;
  private static final int OB_EXTEND_BARS = 2;
  private static final int MAX_STRUCT_LINES = 120;
  private static final int MAX_STRUCT_LABELS = 240;

  enum Values { ATR_TREND }

  enum Signals {
    LONG,
    SHORT,
    BULL_BOS,
    BEAR_BOS,
    BULL_CHOCH,
    BEAR_CHOCH,
    SL_HIT,
    TP1_HIT,
    TP2_HIT,
    TP3_HIT,
    BREAK_EVEN,
    OB_MITIGATED
  }


  @Override
  public void initialize(Defaults defaults)
  {
    SettingsDescriptor sd = createSD();

    SettingTab structure = sd.addTab("Structure");
    SettingGroup sg = structure.addGroup("Structure");
    sg.addRow(new IntegerDescriptor(SWING_LEN, "Swing Length", 13, 2, 50, 1));
    sg.addRow(new DiscreteDescriptor(BREAK_SRC, "Break Confirmation", "Close", opts("Close", "Wick")));
    sg.addRow(new DiscreteDescriptor(SIGNAL_MODE, "Signal Mode", "BOS + CHoCH", opts("BOS + CHoCH", "CHoCH only", "BOS only")));
    sg.addRow(new DiscreteDescriptor(SIGNAL_SOURCE, "Confirmed Signal Source", "Structure + Forge", opts("Structure + Forge", "Structure only", "Forge only")));
    sg.addRow(new DiscreteDescriptor(SIGNAL_GROUP, "Signal Group", "Manual", opts("Manual", "Trend Confirmation", "Momentum Pullback", "Mean Reversion", "Structure Only", "Balanced")));
    sg.addRow(new BooleanDescriptor(SHOW_STRUCT, "Show HH/HL/LH/LL", true));
    sg.addRow(new BooleanDescriptor(SHOW_BOS, "Show BOS/CHoCH", true));

    SettingTab obTab = sd.addTab("Order Blocks");
    SettingGroup og = obTab.addGroup("Suggested Order Blocks");
    og.addRow(new BooleanDescriptor(SHOW_OB, "Show Order Blocks", true));
    og.addRow(new DiscreteDescriptor(OB_FROM, "Create OB On", "BOS + CHoCH", opts("BOS + CHoCH", "CHoCH only", "BOS only")));
    og.addRow(new DiscreteDescriptor(OB_MIT_SRC, "Mitigation Trigger", "Wick", opts("Wick", "Close")));
    og.addRow(new BooleanDescriptor(REMOVE_MIT, "Remove OB After Mitigation", false));
    og.addRow(new IntegerDescriptor(OB_LOOKBACK, "OB Lookback (bars)", 30, 3, 100, 1));
    og.addRow(new IntegerDescriptor(MAX_OB, "Max Order Blocks", 8, 1, 30, 1));
    og.addRow(new IntegerDescriptor(OB_ALPHA, "OB Fill Alpha", 38, 5, 180, 1));
    og.addRow(new BooleanDescriptor(OB_MEAN, "Show 50% Mean Line", false));
    og.addRow(new BooleanDescriptor(OB_LABELS, "Show OB Labels", false));

    SettingTab filters = sd.addTab("Filters");
    SettingGroup htf = filters.addGroup("HTF Trend Filter");
    htf.addRow(new BooleanDescriptor(USE_HTF, "HTF Trend Filter", false));
    htf.addRow(new BarSizeDescriptor(HTF_BAR_SIZE, "HTF Timeframe", BarSize.minute(60)));
    htf.addRow(new IntegerDescriptor(HTF_EMA_LEN, "HTF EMA Length", 50, 5, 200, 1));

    SettingGroup forge = filters.addGroup("Signals/Filters");
    forge.addRow(new BooleanDescriptor(REQUIRE_ALL, "Require All Enabled Indicators", true));
    forge.addRow(new BooleanDescriptor(ENABLE_SMA, "Enable SMA Crossover", true),
      new IntegerDescriptor(SMA_FAST, "Fast", 10, 1, 300, 1),
      new IntegerDescriptor(SMA_SLOW, "Slow", 20, 1, 300, 1));
    forge.addRow(new BooleanDescriptor(ENABLE_RSI, "Enable RSI Filter", false),
      new IntegerDescriptor(RSI_LEN, "Length", 14, 1, 200, 1),
      new DoubleDescriptor(RSI_LONG, "Long Above", 50, 1, 99, 0.5),
      new DoubleDescriptor(RSI_SHORT, "Short Below", 50, 1, 99, 0.5));
    forge.addRow(new BooleanDescriptor(ENABLE_MACD, "Enable MACD Crossover", false),
      new IntegerDescriptor(MACD_FAST, "Fast", 12, 1, 200, 1),
      new IntegerDescriptor(MACD_SLOW, "Slow", 26, 1, 300, 1),
      new IntegerDescriptor(MACD_SIGNAL, "Signal", 9, 1, 100, 1));
    forge.addRow(new BooleanDescriptor(ENABLE_ST, "Enable Supertrend", false),
      new DoubleDescriptor(ST_FACTOR, "Factor", 3.0, 0.1, 20.0, 0.1),
      new IntegerDescriptor(ST_LEN, "Length", 10, 1, 200, 1));
    forge.addRow(new BooleanDescriptor(ENABLE_STOCH, "Enable Stochastic", false),
      new IntegerDescriptor(STOCH_K, "K", 14, 1, 200, 1),
      new IntegerDescriptor(STOCH_D, "D", 3, 1, 50, 1),
      new IntegerDescriptor(STOCH_SMOOTH, "Smooth", 3, 1, 50, 1));
    forge.addRow(new BooleanDescriptor(ENABLE_BB, "Enable BB Trend", false),
      new IntegerDescriptor(BB_LEN, "Length", 20, 1, 300, 1),
      new DoubleDescriptor(BB_MULT, "Multiplier", 2.0, 0.1, 10.0, 0.1));
    forge.addRow(new BooleanDescriptor(ENABLE_EMA, "Enable EMA Crossover", false),
      new IntegerDescriptor(EMA_FAST, "Fast", 10, 1, 300, 1),
      new IntegerDescriptor(EMA_SLOW, "Slow", 20, 1, 300, 1));
    forge.addRow(new BooleanDescriptor(ENABLE_AO, "Enable AO", false));
    forge.addRow(new BooleanDescriptor(ENABLE_SAR, "Enable Parabolic SAR", false),
      new DoubleDescriptor(SAR_START, "Start", 0.02, 0.001, 1.0, 0.01),
      new DoubleDescriptor(SAR_INC, "Increment", 0.02, 0.001, 1.0, 0.01),
      new DoubleDescriptor(SAR_MAX, "Max", 0.2, 0.01, 2.0, 0.01));
    forge.addRow(new BooleanDescriptor(ENABLE_CCI, "Enable CCI Filter", false),
      new IntegerDescriptor(CCI_LEN, "Length", 20, 1, 300, 1),
      new DoubleDescriptor(CCI_LONG, "Long Above", 0, -300, 300, 1),
      new DoubleDescriptor(CCI_SHORT, "Short Below", 0, -300, 300, 1));
    forge.addRow(new BooleanDescriptor(ENABLE_ADX, "Enable ADX Filter", false),
      new IntegerDescriptor(DI_LEN, "DI Length", 14, 1, 200, 1),
      new IntegerDescriptor(ADX_LEN, "ADX Smoothing", 14, 1, 200, 1),
      new DoubleDescriptor(ADX_THRESHOLD, "Threshold", 20, 1, 100, 1));

    SettingGroup strategyFilters = filters.addGroup("Strategy Filters");
    strategyFilters.addRow(new BooleanDescriptor(ENABLE_TILSON, "Enable Tilson IE/2", false),
      new DiscreteDescriptor(TILSON_INPUT, "Source", "High", opts("Close", "Open", "High", "Low", "HL2", "HLC3")),
      new DiscreteDescriptor(TILSON_METHOD, "Method", "MEMA", opts("SMA", "EMA", "MEMA")),
      new IntegerDescriptor(TILSON_PERIOD, "Period", 12, 2, 200, 1));
    strategyFilters.addRow(new BooleanDescriptor(ENABLE_SMI, "Enable SMI Ergodic", false),
      new DiscreteDescriptor(SMI_INPUT, "Source", "Open", opts("Close", "Open", "High", "Low", "HL2", "HLC3")),
      new DiscreteDescriptor(SMI_METHOD, "Method", "SMA", opts("SMA", "EMA", "MEMA")),
      new IntegerDescriptor(SMI_LONG_PERIOD, "Long", 10, 1, 200, 1),
      new IntegerDescriptor(SMI_SHORT_PERIOD, "Short", 12, 1, 200, 1),
      new IntegerDescriptor(SMI_SIGNAL_PERIOD, "Signal", 6, 1, 100, 1));
    strategyFilters.addRow(new DiscreteDescriptor(SMI_MODE, "SMI Mode", "Line vs Signal",
      opts("Line vs Signal", "Zero Bias", "Guided Reversal")),
      new DoubleDescriptor(SMI_TOP_GUIDE, "Top Guide", 0.1, -10.0, 10.0, 0.1),
      new DoubleDescriptor(SMI_BOTTOM_GUIDE, "Bottom Guide", -0.1, -10.0, 10.0, 0.1));

    SettingTab risk = sd.addTab("Risk");
    SettingGroup rg = risk.addGroup("Risk Management");
    rg.addRow(new DiscreteDescriptor(RISK_PRESET, "Risk Preset", "Balanced", opts("Conservative", "Balanced", "Aggressive", "Scalping", "Custom")));
    rg.addRow(new DiscreteDescriptor(TP_MODE, "Take Profit Mode", "Three Targets", opts("Single Target", "Three Targets")));
    rg.addRow(new IntegerDescriptor(ATR_RISK_LEN, "ATR Length (SL)", 13, 5, 50, 1));
    rg.addRow(new DoubleDescriptor(SL_MULT, "SL x ATR (Custom)", 1.5, 0.5, 5.0, 0.1),
      new DoubleDescriptor(TP_MULT, "Single TP x Risk (Custom)", 2.0, 0.5, 15.0, 0.1));
    rg.addRow(new DoubleDescriptor(TP1_MULT, "TP1 x Risk (Custom)", 1.0, 0.5, 5.0, 0.1));
    rg.addRow(new DoubleDescriptor(TP2_MULT, "TP2 x Risk (Custom)", 2.0, 1.0, 10.0, 0.1));
    rg.addRow(new DoubleDescriptor(TP3_MULT, "TP3 x Risk (Custom)", 3.0, 1.5, 15.0, 0.1));
    rg.addRow(new BooleanDescriptor(SHOW_RISK, "Show SL/TP Lines", true));
    rg.addRow(new BooleanDescriptor(USE_BE, "Break-Even After TP1", true));

    SettingTab optimizer = sd.addTab("Optimizer");
    SettingGroup optg = optimizer.addGroup("Chart Period Optimizer");
    optg.addRow(new BooleanDescriptor(SHOW_OPTIMIZER, "Show Optimizer Suggestions", false),
      new BooleanDescriptor(APPLY_OPTIMIZER, "Apply Optimizer Recommendation Now", false));
    optg.addRow(new IntegerDescriptor(OPT_LOOKBACK, "Optimization Lookback Bars", 2500, 200, 50000, 100),
      new IntegerDescriptor(OPT_MIN_TRADES, "Minimum Trades", 8, 1, 200, 1));
    optg.addRow(new DiscreteDescriptor(OPT_OBJECTIVE, "Objective", "Balanced",
      opts("Balanced", "Net Points", "Profit Factor", "PF vs Max DD", "Recovery Factor")));
    optg.addRow(new DiscreteDescriptor(OPT_SEARCH, "Search Profile", "NQ 5/15m Fast", opts("NQ 5/15m Fast", "Around Current")));
    optg.addRow(new DiscreteDescriptor(OPT_REFRESH_MODE, "Refresh Mode", "Every Bar",
      opts("Every Bar", "Every N Bars", "On Demand")),
      new IntegerDescriptor(OPT_REFRESH_INTERVAL, "Refresh Every N Bars", 5, 1, 500, 1));
    optg.addRow(new DiscreteDescriptor(OPT_DEPTH, "Optimizer Depth", "Fast", opts("Fast", "Medium", "Deep")));

    SettingTab visual = sd.addTab("Visual");
    SettingGroup vg = visual.addGroup("Display");
    vg.addRow(new BooleanDescriptor(SHOW_DASHBOARD, "Show Live Backtest Dashboard", true),
      new DiscreteDescriptor(DASHBOARD_MODE, "Dashboard Mode", "Full", opts("Full", "Compact")),
      new BooleanDescriptor(DASHBOARD_HIDE_UNUSED, "Hide Unused Dashboard Items", false));
    vg.addRow(new DiscreteDescriptor(DASHBOARD_POS_PRESET, "Dashboard Position", "Top Left",
      opts("Top Left", "Top Right", "Bottom Left", "Bottom Right", "Custom")));
    vg.addRow(new IntegerDescriptor(DASHBOARD_X_OFFSET, "Dashboard X Offset", 12, 0, 200, 1),
      new IntegerDescriptor(DASHBOARD_Y_OFFSET, "Dashboard Y Offset", 12, 0, 200, 1));
    vg.addRow(new IntegerDescriptor(DASHBOARD_SCALE, "Dashboard Width Scale %", 100, 50, 200, 5));
    vg.addRow(new BooleanDescriptor(SHOW_PROJECTION, "Show Next Trade Projection", true),
      new IntegerDescriptor(PROJECTION_BARS, "Projection Bars", 16, 4, 80, 1));
    vg.addRow(new IntegerDescriptor(DASHBOARD_LOOKBACK, "Backtest Lookback Bars", 5000, 100, 50000, 100));
    vg.addRow(new BooleanDescriptor(SHOW_ATR_TREND, "Show ATR Trend Line", true));
    vg.addRow(new IntegerDescriptor(ATR_TREND_LEN, "ATR Trend Length", 10, 1, 100, 1));
    vg.addRow(new DoubleDescriptor(ATR_TREND_MULT, "ATR Trend Multiplier", 5.0, 0.5, 10.0, 0.1));
    vg.addRow(new IntegerDescriptor(ATR_SMOOTH, "ATR Line Smoothing", 5, 1, 50, 1));
    vg.addRow(new ColorDescriptor(BULL_COLOR, "Bull Color", new Color(0, 230, 118)));
    vg.addRow(new ColorDescriptor(BEAR_COLOR, "Bear Color", new Color(255, 82, 82)));
    vg.addRow(new ColorDescriptor(OB_BULL_COLOR, "OB Bull", new Color(38, 166, 154)));
    vg.addRow(new ColorDescriptor(OB_BEAR_COLOR, "OB Bear", new Color(239, 83, 80)));
    vg.addRow(new MarkerDescriptor(UP_MARKER, "Long Marker", Enums$MarkerType.TRIANGLE, Enums$Size.SMALL,
      new Color(0, 230, 118), Color.BLACK, true, true));
    vg.addRow(new MarkerDescriptor(DOWN_MARKER, "Short Marker", Enums$MarkerType.TRIANGLE, Enums$Size.SMALL,
      new Color(255, 82, 82), Color.BLACK, true, true));
    vg.addRow(new PathDescriptor(ATR_TREND_PATH, "ATR Trend", new Color(0, 230, 118), 2.0f, null));
    vg.addRow(new PathDescriptor(STRUCT_PATH, "Structure Lines", new Color(128, 128, 128), 1.5f, new float[] {3f, 3f}));
    vg.addRow(new PathDescriptor(RISK_PATH, "Risk Lines", new Color(80, 140, 180), 1.0f, null));

    SettingTab alerts = sd.addTab("Alerts");
    SettingGroup ag = alerts.addGroup("Alerts");
    ag.addRow(new BooleanDescriptor(ALERT_SL, "Alert on SL Hit", true));
    ag.addRow(new BooleanDescriptor(ALERT_TP, "Alert on TP / Break-Even", false));
    sd.addQuickSettings(SWING_LEN, BREAK_SRC, SIGNAL_SOURCE, SIGNAL_MODE, SIGNAL_GROUP, SHOW_OB, MAX_OB, ENABLE_SMA, ENABLE_ST, ENABLE_TILSON, ENABLE_SMI, USE_HTF, TP_MODE, SHOW_DASHBOARD, DASHBOARD_MODE, SHOW_OPTIMIZER, APPLY_OPTIMIZER, OPT_DEPTH, SHOW_PROJECTION);


    RuntimeDescriptor rd = createRD();
    rd.setLabelSettings(SWING_LEN, SIGNAL_SOURCE, SIGNAL_MODE, SIGNAL_GROUP);
    rd.exportValue(new ValueDescriptor(Values.ATR_TREND, "ATR Trend", ATR_TREND_LEN, ATR_TREND_MULT));
    rd.declarePath(Values.ATR_TREND, ATR_TREND_PATH);
    rd.declareIndicator(Values.ATR_TREND, Inputs.IND);
    rd.declareSignal(Signals.LONG, "Long Confirmed");
    rd.declareSignal(Signals.SHORT, "Short Confirmed");
    rd.declareSignal(Signals.BULL_BOS, "Bull BOS");
    rd.declareSignal(Signals.BEAR_BOS, "Bear BOS");
    rd.declareSignal(Signals.BULL_CHOCH, "Bull CHoCH");
    rd.declareSignal(Signals.BEAR_CHOCH, "Bear CHoCH");
    rd.declareSignal(Signals.SL_HIT, "SL Hit");
    rd.declareSignal(Signals.TP1_HIT, "TP1 Hit");
    rd.declareSignal(Signals.TP2_HIT, "TP2 Hit");
    rd.declareSignal(Signals.TP3_HIT, "TP3 Hit");
    rd.declareSignal(Signals.BREAK_EVEN, "Break Even");
    rd.declareSignal(Signals.OB_MITIGATED, "OB Mitigated");
    rd.setRangeKeys(Values.ATR_TREND);
  }

  @Override
  public void onLoad(Defaults defaults)
  {
    setMinBars(60);
    System.out.println("Meridian Flow Forge " + VERSION + " loaded");
  }

  @Override
  public boolean isRepaintAllOnUpdate()
  {
    return false;
  }

  @Override
  public boolean onClick(Point p, int button) {
    DashboardFigure fig = lastDashboardFigure;
    if (fig == null) return false;
    Rectangle2D bounds = fig.applyBounds();
    if (bounds == null) return false;
    if (!bounds.contains(p.x, p.y)) return false;

    SettingsView cfg = lastDashboardCfg;
    int signalIndex = lastDashboardSignalIndex;
    if (cfg == null || signalIndex < 0) return false;

    DataContext ctx = getDataContext();
    DataSeries s = ctx.getDataSeries();
    optimizer.apply(ctx, s, cfg, signalIndex, getSettings());
    calculationCacheKey = "";
    return true;
  }
  @Override
  protected void calculateValues(DataContext ctx)
  {
    DataSeries s = ctx.getDataSeries();
    int n = s.size();
    if (n < 5) return;
    if (!loggedCalculate) {
      loggedCalculate = true;
      System.out.println("Meridian Flow Forge " + VERSION + " calculating " + n + " bars");
    }

    SettingsView cfg = new SettingsView();
    cfg.read(getSettings(), ctx);
    int signalIndex = latestCompleteIndex(s);
    if (getSettings().getBoolean(APPLY_OPTIMIZER, false)) {
      OptimizerResult applied = optimizer.apply(ctx, s, cfg, signalIndex, getSettings());
      if (applied != null && applied.valid && applied.cfg != null) { cfg = applied.cfg; calculationCacheKey = ""; }
    }
    String calcKey = calculationKey(s, cfg, signalIndex);
    if (calcKey.equals(calculationCacheKey)) return;

    beginFigureUpdate();
    clearFigures();

    boolean needsForge = !"Structure only".equals(cfg.signalSource);
    boolean needsDashboardFilters = cfg.showDashboard && !cfg.dashboardCompact;
    double[] closes = closeArray(s);
    double[] smaFast = needsForge && cfg.enableSma ? sma(closes, cfg.smaFast) : null;
    double[] smaSlow = needsForge && cfg.enableSma ? sma(closes, cfg.smaSlow) : null;
    double[] emaFast = needsForge && cfg.enableEma ? ema(closes, cfg.emaFast) : null;
    double[] emaSlow = needsForge && cfg.enableEma ? ema(closes, cfg.emaSlow) : null;
    double[] rsi = (needsForge || needsDashboardFilters) && cfg.enableRsi ? rsi(s, cfg.rsiLen) : null;
    Macd macd = needsForge && cfg.enableMacd ? macd(s, cfg.macdFast, cfg.macdSlow, cfg.macdSignal) : null;
    Stoch stoch = (needsForge || needsDashboardFilters) && cfg.enableStoch ? stoch(s, cfg.stochK, cfg.stochD, cfg.stochSmooth) : null;
    Bands bb = needsForge && cfg.enableBb ? bollinger(closes, cfg.bbLen, cfg.bbMult) : null;
    double[] ao = needsForge && cfg.enableAo ? ao(s) : null;
    double[] cci = needsForge && cfg.enableCci ? cci(s, cfg.cciLen) : null;
    Adx adx = needsForge && cfg.enableAdx ? adx(s, cfg.diLen, cfg.adxLen) : null;
    Sar sar = (needsForge || needsDashboardFilters) && cfg.enableSar ? sar(s, cfg.sarStart, cfg.sarInc, cfg.sarMax) : null;
    Tilson tilson = (needsForge || needsDashboardFilters) && cfg.enableTilson ? tilson(s, cfg.tilsonInput, cfg.tilsonMethod, cfg.tilsonPeriod) : null;
    Smi smi = (needsForge || needsDashboardFilters) && cfg.enableSmi ? smi(s, cfg.smiInput, cfg.smiMethod, cfg.smiLongPeriod, cfg.smiShortPeriod, cfg.smiSignalPeriod) : null;
    Super superTrend = needsForge && cfg.enableSt ? superTrend(s, cfg.stLen, cfg.stFactor) : null;
    double[] atrRisk = atr(s, cfg.atrRiskLen);
    double[] atrTrendSmooth = cfg.showAtrTrend ? ema(atr(s, cfg.atrTrendLen), cfg.atrSmooth) : null;

    boolean[] forgeLong = new boolean[n];
    boolean[] openLongSignals = new boolean[n];
    boolean[] openShortSignals = new boolean[n];

    boolean[] forgeShort = new boolean[n];
    if (needsForge) {
      for (int i = 0; i < n; i++) {
        ForgeState state = forgeState(cfg, i, smaFast, smaSlow, emaFast, emaSlow, rsi, macd,
          stoch, bb, closes, ao, sar, cci, adx, superTrend, tilson, smi);
        forgeLong[i] = state.longOk;
        forgeShort[i] = state.shortOk;
      }
    }

    HtfBias htfBias = buildHtfBias(cfg, ctx, s, n);

    int warmup = Math.max(cfg.swingLen * 2, 50);
    double lastSwingHigh = Double.NaN;
    int lastSwingHighBar = -1;
    double prevSwingHigh = Double.NaN;
    boolean swingHighBroken = true;
    double lastSwingLow = Double.NaN;
    int lastSwingLowBar = -1;
    double prevSwingLow = Double.NaN;
    boolean swingLowBroken = true;
    int structTrend = 0;
    int prevStructTrend = 0;
    List<OBZone> zones = new ArrayList<>();
    List<FigureEvent> structLineEvents = new ArrayList<>();
    List<FigureEvent> structLabelEvents = new ArrayList<>();

    int activeDir = 0;
    int entryBar = -1;
    double activeEntry = Double.NaN;
    double activeSL = Double.NaN;
    double activeTP1 = Double.NaN;
    double activeTP2 = Double.NaN;
    double activeTP3 = Double.NaN;
    boolean tp1Reached = false;
    boolean tp2Reached = false;
    boolean tp3Reached = false;
    boolean beActive = false;
    TradeLines lastTrade = null;

    double atrTrendLine = Double.NaN;
    double prevAtrTrendLine = Double.NaN;
    PathInfo structPath = getSettings().getPath(STRUCT_PATH);

    for (int i = 0; i < n; i++) {
      boolean confirmed = s.isBarComplete(i);
      int pivotBar = i - cfg.swingLen;
      if (pivotBar >= cfg.swingLen && pivotBar + cfg.swingLen <= signalIndex) {
        if (isPivotHigh(s, pivotBar, cfg.swingLen)) {
          prevSwingHigh = lastSwingHigh;
          lastSwingHigh = s.getHigh(pivotBar);
          lastSwingHighBar = pivotBar;
          swingHighBroken = false;
          if (cfg.showStruct) {
            boolean hh = Double.isNaN(prevSwingHigh) || lastSwingHigh >= prevSwingHigh;
            addLimited(structLabelEvents, MAX_STRUCT_LABELS,
              FigureEvent.label(pivotBar, lastSwingHigh, hh ? "HH" : "LH", hh ? cfg.bullColor : cfg.neutralColor, Enums$Position.TOP));
          }
        }
        if (isPivotLow(s, pivotBar, cfg.swingLen)) {
          prevSwingLow = lastSwingLow;
          lastSwingLow = s.getLow(pivotBar);
          lastSwingLowBar = pivotBar;
          swingLowBroken = false;
          if (cfg.showStruct) {
            boolean ll = Double.isNaN(prevSwingLow) || lastSwingLow <= prevSwingLow;
            addLimited(structLabelEvents, MAX_STRUCT_LABELS,
              FigureEvent.label(pivotBar, lastSwingLow, ll ? "LL" : "HL", ll ? cfg.bearColor : cfg.bullColor, Enums$Position.BOTTOM));
          }
        }
      }

      double breakHighSrc = cfg.breakOnWick ? s.getHigh(i) : s.getClose(i);
      double breakLowSrc = cfg.breakOnWick ? s.getLow(i) : s.getClose(i);
      boolean rawBullBreak = !Double.isNaN(lastSwingHigh) && !swingHighBroken && breakHighSrc > lastSwingHigh;
      boolean rawBearBreak = !Double.isNaN(lastSwingLow) && !swingLowBroken && breakLowSrc < lastSwingLow;
      boolean conflict = rawBullBreak && rawBearBreak;
      boolean bullBreak = rawBullBreak && !conflict && confirmed && i >= warmup;
      boolean bearBreak = rawBearBreak && !conflict && confirmed && i >= warmup;

      boolean isBullBos = false, isBullChoch = false, isBearBos = false, isBearChoch = false;
      double brokenHighLvl = Double.NaN, brokenLowLvl = Double.NaN;
      int brokenHighBar = -1, brokenLowBar = -1;

      if (bullBreak) {
        isBullBos = structTrend >= 0;
        isBullChoch = structTrend < 0;
        brokenHighBar = lastSwingHighBar;
        brokenHighLvl = lastSwingHigh;
        prevStructTrend = structTrend;
        structTrend = 1;
        swingHighBroken = true;
      }
      if (bearBreak) {
        isBearBos = structTrend <= 0;
        isBearChoch = structTrend > 0;
        brokenLowBar = lastSwingLowBar;
        brokenLowLvl = lastSwingLow;
        prevStructTrend = structTrend;
        structTrend = -1;
        swingLowBroken = true;
      }

      if (cfg.showBos && (isBullBos || isBullChoch) && brokenHighBar >= 0) {
        if (structPath == null || structPath.isEnabled()) {
          addLimited(structLineEvents, MAX_STRUCT_LINES,
            FigureEvent.line(brokenHighBar, brokenHighLvl, i, brokenHighLvl, structColor(structPath), structStroke(structPath)));
        }
        addLimited(structLabelEvents, MAX_STRUCT_LABELS,
          FigureEvent.label(i, brokenHighLvl, isBullChoch ? "CHoCH UP" : "BOS UP", cfg.bullColor, Enums$Position.TOP));
      }
      if (cfg.showBos && (isBearBos || isBearChoch) && brokenLowBar >= 0) {
        if (structPath == null || structPath.isEnabled()) {
          addLimited(structLineEvents, MAX_STRUCT_LINES,
            FigureEvent.line(brokenLowBar, brokenLowLvl, i, brokenLowLvl, structColor(structPath), structStroke(structPath)));
        }
        addLimited(structLabelEvents, MAX_STRUCT_LABELS,
          FigureEvent.label(i, brokenLowLvl, isBearChoch ? "CHoCH DOWN" : "BOS DOWN", cfg.bearColor, Enums$Position.BOTTOM));
      }

      if (i == signalIndex) {
        if (isBullBos && eventAllowed(cfg.signalMode, true, false)) signal(ctx, i, Signals.BULL_BOS, "Bull BOS above " + formatPrice(brokenHighLvl), brokenHighLvl);
        if (isBullChoch && eventAllowed(cfg.signalMode, false, true)) signal(ctx, i, Signals.BULL_CHOCH, "Bull CHoCH above " + formatPrice(brokenHighLvl), brokenHighLvl);
        if (isBearBos && eventAllowed(cfg.signalMode, true, false)) signal(ctx, i, Signals.BEAR_BOS, "Bear BOS below " + formatPrice(brokenLowLvl), brokenLowLvl);
        if (isBearChoch && eventAllowed(cfg.signalMode, false, true)) signal(ctx, i, Signals.BEAR_CHOCH, "Bear CHoCH below " + formatPrice(brokenLowLvl), brokenLowLvl);
      }

      boolean bullObEvent = eventAllowed(cfg.obFrom, isBullBos, isBullChoch);
      boolean bearObEvent = eventAllowed(cfg.obFrom, isBearBos, isBearChoch);
      if (cfg.showOB && bullObEvent) {
        int obBar = findObBar(s, i, true, cfg.obLookback);
        if (obBar >= 0) spawnOB(s, zones, 1, obBar, i, cfg);
      }
      if (cfg.showOB && bearObEvent) {
        int obBar = findObBar(s, i, false, cfg.obLookback);
        if (obBar >= 0) spawnOB(s, zones, -1, obBar, i, cfg);
      }

      maintainOBs(ctx, s, zones, i, cfg, signalIndex);

      boolean bullEvent = eventAllowed(cfg.signalMode, isBullBos, isBullChoch);
      boolean bearEvent = eventAllowed(cfg.signalMode, isBearBos, isBearChoch);
      boolean htfBullOk = !cfg.useHtf || htfBias.bull[i];
      boolean htfBearOk = !cfg.useHtf || htfBias.bear[i];
      boolean forgeLongRising = forgeLong[i] && (i == 0 || !forgeLong[i - 1]);
      boolean forgeShortRising = forgeShort[i] && (i == 0 || !forgeShort[i - 1]);
      boolean openLongSignal = switch (cfg.signalSource) {
        case "Structure only" -> bullEvent && htfBullOk;
        case "Forge only" -> forgeLongRising && htfBullOk;
        default -> bullEvent && htfBullOk && forgeLong[i];
      };
      boolean openShortSignal = switch (cfg.signalSource) {
        case "Structure only" -> bearEvent && htfBearOk;
        case "Forge only" -> forgeShortRising && htfBearOk;
        default -> bearEvent && htfBearOk && forgeShort[i];
      };
      openLongSignals[i] = openLongSignal;
      openShortSignals[i] = openShortSignal;

      // Suppress conflicting long/short signals: store neither on a tie.
      if (openLongSignal && openShortSignal) {
        openLongSignal = openShortSignal = false;
        openLongSignals[i] = false;
        openShortSignals[i] = false;
      }


      double riskAtr = nz(atrRisk[i]);
      double slDistance = riskAtr * cfg.slMultEff;
      boolean openLong = openLongSignal && slDistance > 0 && activeDir == 0 && confirmed;
      boolean openShort = openShortSignal && slDistance > 0 && activeDir == 0 && confirmed;

      if (openLong || openShort) {
        activeDir = openLong ? 1 : -1;
        activeEntry = s.getClose(i);
        activeSL = activeEntry - activeDir * slDistance;
        activeTP1 = activeEntry + activeDir * slDistance * (cfg.singleTarget ? cfg.tpEff : cfg.tp1Eff);
        activeTP2 = cfg.singleTarget ? Double.NaN : activeEntry + activeDir * slDistance * cfg.tp2Eff;
        activeTP3 = cfg.singleTarget ? Double.NaN : activeEntry + activeDir * slDistance * cfg.tp3Eff;
        entryBar = i;
        tp1Reached = false;
        tp2Reached = false;
        tp3Reached = false;
        beActive = false;
        lastTrade = snapshot(activeDir, i, i + LINE_FORWARD_BARS, activeEntry, activeSL, activeTP1, activeTP2, activeTP3,
          tp1Reached, tp2Reached, tp3Reached, beActive);
        MarkerInfo markerInfo = getSettings().getMarker(openLong ? UP_MARKER : DOWN_MARKER);
        if (markerInfo == null) {
          MarkerInfo fallback = new MarkerInfo(Enums$MarkerType.TRIANGLE, Enums$Size.SMALL,
            openLong ? cfg.bullColor : cfg.bearColor, Color.BLACK, true);
          Marker marker = new Marker(new Coordinate(s.getStartTime(i), openLong ? s.getLow(i) : s.getHigh(i)),
            openLong ? Enums$Position.BOTTOM : Enums$Position.TOP, fallback, openLong ? "Long" : "Short");
          addFigure(marker);
        } else if (markerInfo.isEnabled()) {
          Marker marker = new Marker(new Coordinate(s.getStartTime(i), openLong ? s.getLow(i) : s.getHigh(i)),
            openLong ? Enums$Position.BOTTOM : Enums$Position.TOP, markerInfo, openLong ? "Long" : "Short");
          addFigure(marker);
        }
        if (i == signalIndex) {
          String msg = (openLong ? "LONG" : "SHORT") + " confirmed | Entry " + formatPrice(activeEntry)
            + " | SL " + formatPrice(activeSL) + targetMessage(cfg, activeTP1, activeTP2, activeTP3);
          signal(ctx, i, openLong ? Signals.LONG : Signals.SHORT, msg, activeEntry);
        }
      }

      boolean canCheckHit = activeDir != 0 && entryBar >= 0 && i > entryBar && confirmed;
      boolean slHit = canCheckHit && (activeDir == 1 ? s.getLow(i) <= activeSL : s.getHigh(i) >= activeSL);
      boolean tp1Hit = canCheckHit && !Double.isNaN(activeTP1) && (activeDir == 1 ? s.getHigh(i) >= activeTP1 : s.getLow(i) <= activeTP1);
      boolean tp2Hit = canCheckHit && !Double.isNaN(activeTP2) && (activeDir == 1 ? s.getHigh(i) >= activeTP2 : s.getLow(i) <= activeTP2);
      boolean tp3Hit = canCheckHit && !Double.isNaN(activeTP3) && (activeDir == 1 ? s.getHigh(i) >= activeTP3 : s.getLow(i) <= activeTP3);

      boolean tp1First = targetHitBeforeStop(slHit, tp1Hit) && !tp1Reached;
      boolean tp2First = targetHitBeforeStop(slHit, tp2Hit) && !tp2Reached;
      boolean tp3First = targetHitBeforeStop(slHit, tp3Hit) && !tp3Reached;
      if (tp1First) tp1Reached = true;
      if (tp2First) tp2Reached = true;
      if (tp3First) tp3Reached = true;

      boolean beJustActivated = false;
      if (!cfg.singleTarget && cfg.useBreakEven && tp1First && !beActive) {
        activeSL = activeEntry;
        beActive = true;
        beJustActivated = true;
      }

      if (i == signalIndex && cfg.alertTp) {
        if (tp1First) signal(ctx, i, Signals.TP1_HIT, (cfg.singleTarget ? "TP hit " : "TP1 hit ") + formatPrice(activeTP1), activeTP1);
        if (!cfg.singleTarget && tp2First) signal(ctx, i, Signals.TP2_HIT, "TP2 hit " + formatPrice(activeTP2), activeTP2);
        if (!cfg.singleTarget && tp3First) signal(ctx, i, Signals.TP3_HIT, "TP3 hit " + formatPrice(activeTP3), activeTP3);
        if (beJustActivated) signal(ctx, i, Signals.BREAK_EVEN, "Break-even: SL moved to entry " + formatPrice(activeEntry), activeEntry);
      }

      if (activeDir != 0 && lastTrade != null) {
        lastTrade.sl = activeSL;
        lastTrade.endIndex = i + LINE_UPDATE_BARS;
        lastTrade.tp1Hit = tp1Reached;
        lastTrade.tp2Hit = tp2Reached;
        lastTrade.tp3Hit = tp3Reached;
        lastTrade.beActive = beActive;
      }

      if ((slHit || (cfg.singleTarget ? tp1Hit : tp3Hit)) && activeDir != 0) {
        if (i == signalIndex && slHit && cfg.alertSl) {
          signal(ctx, i, Signals.SL_HIT, (beActive ? "BE stop-out " : "SL hit ") + formatPrice(activeSL), activeSL);
        }
        activeDir = 0;
        entryBar = -1;
        activeEntry = Double.NaN;
        activeSL = Double.NaN;
        activeTP1 = Double.NaN;
        activeTP2 = Double.NaN;
        activeTP3 = Double.NaN;
        tp1Reached = false;
        tp2Reached = false;
        tp3Reached = false;
        beActive = false;
      }

      if (cfg.showAtrTrend) {
        double atrTl = nz(atrTrendSmooth[i]);
        double src = (s.getHigh(i) + s.getLow(i)) / 2.0;
        double longBand = src - cfg.atrTrendMult * atrTl;
        double shortBand = src + cfg.atrTrendMult * atrTl;
        boolean atrFlip = structTrend != prevStructTrend && structTrend != 0;
        if (structTrend == 1) {
          atrTrendLine = atrFlip || Double.isNaN(prevAtrTrendLine) ? longBand : Math.max(prevAtrTrendLine, longBand);
        }
        else if (structTrend == -1) {
          atrTrendLine = atrFlip || Double.isNaN(prevAtrTrendLine) ? shortBand : Math.min(prevAtrTrendLine, shortBand);
        }
        else {
          atrTrendLine = Double.NaN;
        }
        if (!atrFlip && !Double.isNaN(atrTrendLine)) {
          s.setDouble(i, Values.ATR_TREND, atrTrendLine);
          s.setPathColor(i, Values.ATR_TREND, structTrend == 1 ? cfg.bullColor : cfg.bearColor);
        }
        else {
          s.setDouble(i, Values.ATR_TREND, null);
        }
        if (atrFlip && !Double.isNaN(atrTrendLine)) {
          Marker flip = new Marker(new Coordinate(s.getStartTime(i), atrTrendLine), Enums$MarkerType.CIRCLE,
            Enums$Size.VERY_SMALL, Enums$Position.CENTER, structTrend == 1 ? cfg.bullColor : cfg.bearColor, Color.BLACK);
          addFigure(flip);
        }
        prevAtrTrendLine = atrTrendLine;
      }
      else {
        s.setDouble(i, Values.ATR_TREND, null);
      }
      prevStructTrend = structTrend;
      s.setComplete(i);
    }

    for (OBZone ob : zones) drawOB(s, ob, cfg);
    Projection projection = cfg.showProjection
      ? buildProjection(s, cfg, signalIndex, activeDir, lastSwingHigh, lastSwingLow, swingHighBroken, swingLowBroken,
        structTrend, forgeLong, forgeShort, htfBias, atrRisk)
      : null;
    for (FigureEvent e : structLineEvents) drawEvent(s, e);
    for (FigureEvent e : structLabelEvents) drawEvent(s, e);
    if (lastTrade != null && cfg.showRisk) drawTradeLines(s, lastTrade, cfg);
    if (projection != null) drawProjection(s, projection, cfg);

    if (cfg.showDashboard) {
      lastDashboardCfg = cfg;
      lastDashboardSignalIndex = signalIndex;
      drawDashboard(ctx, s, cfg, signalIndex, openLongSignals, openShortSignals, atrRisk, rsi, stoch, sar, tilson, smi);
    }

    calculationCacheKey = calcKey;
    endFigureUpdate();
  }

  private void drawEvent(DataSeries s, FigureEvent e) {
    if (e.x1 < 0 || e.x1 >= s.size()) return;
    if (e.type == 1) {
      int x2 = Math.max(0, Math.min(e.x2, s.size() - 1));
      Line line = new Line(s.getStartTime(e.x1), e.y1, s.getStartTime(x2), e.y2);
      line.setColor(alpha(e.color, 190));
      line.setStroke(e.stroke);
      addFigure(line);
    }
    else {
      Label label = new Label(new Coordinate(s.getStartTime(e.x1), e.y1), e.text);
      label.setPosition(e.pos);
      label.getText().setTextColor(e.color);
      label.getText().setBackground(alpha(Color.BLACK, 0));
      label.getText().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
      addFigure(label);
    }
  }

  private void drawOB(DataSeries s, OBZone ob, SettingsView cfg) {
    int right = Math.max(ob.left, Math.min(ob.right, s.size() - 1));
    Color base = ob.dir == 1 ? cfg.obBullColor : cfg.obBearColor;
    Color fill = ob.mitigated ? alpha(Color.GRAY, 35) : alpha(base, cfg.obAlpha);
    Color border = ob.mitigated ? alpha(Color.GRAY, 120) : alpha(base, Math.min(255, cfg.obAlpha + 80));
    Box b = new Box(s.getStartTime(ob.left), ob.top, s.getStartTime(right), ob.bot);
    b.setFillColor(fill);
    b.setLineColor(border);
    b.setStroke(new BasicStroke(1.0f));
    b.setPopupMessage((ob.dir == 1 ? "Bull" : "Bear") + " suggested order block"
      + (ob.mitigated ? " (mitigated)" : ""));
    addFigure(b);
    if (cfg.obMean) {
      double mid = (ob.top + ob.bot) / 2.0;
      Line mean = new Line(s.getStartTime(ob.left), mid, s.getStartTime(right), mid);
      mean.setColor(border);
      mean.setStroke(dotted(1.0f));
      addFigure(mean);
    }
    if (cfg.obLabels) {
      Label label = new Label(new Coordinate(s.getStartTime(ob.left), ob.dir == 1 ? ob.top : ob.bot),
        ob.dir == 1 ? "Bull OB" : "Bear OB");
      label.setPosition(ob.dir == 1 ? Enums$Position.TOP : Enums$Position.BOTTOM);
      label.getText().setTextColor(base);
      label.getText().setBackground(alpha(Color.BLACK, 0));
      label.getText().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
      addFigure(label);
    }
  }

  private void drawTradeLines(DataSeries s, TradeLines t, SettingsView cfg) {
    PathInfo riskPath = getSettings().getPath(RISK_PATH);
    if (riskPath != null && !riskPath.isEnabled()) return;
    int x1 = t.entryIndex;
    int x2 = Math.max(x1 + 1, t.endIndex);
    long t1 = timeAtOrProjected(s, x1);
    long t2 = timeAtOrProjected(s, x2);
    Color entry = riskColor(riskPath);
    Color sl = t.beActive ? new Color(255, 167, 38) : new Color(229, 83, 83);
    Color tp = new Color(76, 175, 80);
    BasicStroke entryStroke = riskStroke(riskPath);
    BasicStroke slStroke = solid(riskWidth(riskPath) * 2.0f);
    BasicStroke tpStroke = dashed(riskWidth(riskPath));
    addLevel(t1, t2, t.entry, "ENTRY " + formatPrice(t.entry), entry, entryStroke);
    addLevel(t1, t2, t.sl, (t.beActive ? "BE " : "SL ") + formatPrice(t.sl), sl, slStroke);
    addLevel(t1, t2, t.tp1, (t.tp1Hit ? (cfg.singleTarget ? "TP HIT " : "TP1 HIT ") : (cfg.singleTarget ? "TP " : "TP1 ")) + formatPrice(t.tp1), t.tp1Hit ? new Color(77, 182, 172) : tp, tpStroke);
    if (!cfg.singleTarget) addLevel(t1, t2, t.tp2, (t.tp2Hit ? "TP2 HIT " : "TP2 ") + formatPrice(t.tp2), t.tp2Hit ? new Color(77, 182, 172) : tp, tpStroke);
    if (!cfg.singleTarget) addLevel(t1, t2, t.tp3, (t.tp3Hit ? "TP3 HIT " : "TP3 ") + formatPrice(t.tp3), t.tp3Hit ? new Color(77, 182, 172) : tp, tpStroke);
  }

  private static Projection buildProjection(DataSeries s, SettingsView cfg, int signalIndex, int activeDir,
                                            double lastSwingHigh, double lastSwingLow,
                                            boolean swingHighBroken, boolean swingLowBroken,
                                            int structTrend, boolean[] forgeLong, boolean[] forgeShort,
                                            HtfBias htfBias, double[] atrRisk) {
    if (signalIndex < 0 || signalIndex >= s.size() || activeDir != 0) return null;
    double atr = atrRisk == null || signalIndex >= atrRisk.length ? 0.0 : nz(atrRisk[signalIndex]);
    double stopDistance = atr * cfg.slMultEff;
    if (stopDistance <= 0.0) return null;

    boolean usesStructure = !"Forge only".equals(cfg.signalSource);
    boolean usesForge = !"Structure only".equals(cfg.signalSource);
    boolean htfLong = !cfg.useHtf || (htfBias != null && signalIndex < htfBias.bull.length && htfBias.bull[signalIndex]);
    boolean htfShort = !cfg.useHtf || (htfBias != null && signalIndex < htfBias.bear.length && htfBias.bear[signalIndex]);
    boolean forgeLongNow = forgeLong != null && signalIndex < forgeLong.length && forgeLong[signalIndex];
    boolean forgeShortNow = forgeShort != null && signalIndex < forgeShort.length && forgeShort[signalIndex];
    boolean forgeLongOk = !usesForge || forgeLongNow;
    boolean forgeShortOk = !usesForge || forgeShortNow;
    boolean structureLongOk = !usesStructure || (!Double.isNaN(lastSwingHigh) && !swingHighBroken &&
      eventAllowed(cfg.signalMode, structTrend >= 0, structTrend < 0));
    boolean structureShortOk = !usesStructure || (!Double.isNaN(lastSwingLow) && !swingLowBroken &&
      eventAllowed(cfg.signalMode, structTrend <= 0, structTrend > 0));
    double close = s.getClose(signalIndex);

    Projection p = new Projection();
    p.index = signalIndex;
    p.endIndex = signalIndex + Math.max(1, cfg.projectionBars);
    if (structureLongOk && htfLong && (forgeLongOk || usesStructure)) {
      p.longEntry = usesStructure ? Math.max(close, lastSwingHigh) : close;
      p.longSL = p.longEntry - stopDistance;
      double risk = p.longEntry - p.longSL;
      if (risk > 0.0) {
        p.longTP1 = p.longEntry + risk * (cfg.singleTarget ? cfg.tpEff : cfg.tp1Eff);
        p.longTP2 = cfg.singleTarget ? Double.NaN : p.longEntry + risk * cfg.tp2Eff;
        p.longTP3 = cfg.singleTarget ? Double.NaN : p.longEntry + risk * cfg.tp3Eff;
        p.longLabel = forgeLongOk ? (usesStructure ? "NEXT LONG > swing" : "NEXT LONG") : "WAIT LONG > swing";
        p.longValid = true;
      }
    }
    if (structureShortOk && htfShort && (forgeShortOk || usesStructure)) {
      p.shortEntry = usesStructure ? Math.min(close, lastSwingLow) : close;
      p.shortSL = p.shortEntry + stopDistance;
      double risk = p.shortSL - p.shortEntry;
      if (risk > 0.0) {
        p.shortTP1 = p.shortEntry - risk * (cfg.singleTarget ? cfg.tpEff : cfg.tp1Eff);
        p.shortTP2 = cfg.singleTarget ? Double.NaN : p.shortEntry - risk * cfg.tp2Eff;
        p.shortTP3 = cfg.singleTarget ? Double.NaN : p.shortEntry - risk * cfg.tp3Eff;
        p.shortLabel = forgeShortOk ? (usesStructure ? "NEXT SHORT < swing" : "NEXT SHORT") : "WAIT SHORT < swing";
        p.shortValid = true;
      }
    }
    return p.longValid || p.shortValid ? p : null;
  }

  private void drawProjection(DataSeries s, Projection p, SettingsView cfg) {
    if (p.longValid) drawProjectedSet(s, p.index, p.endIndex, 1, p.longEntry, p.longSL, p.longTP1, p.longTP2, p.longTP3, p.longLabel, cfg);
    if (p.shortValid) drawProjectedSet(s, p.index, p.endIndex, -1, p.shortEntry, p.shortSL, p.shortTP1, p.shortTP2, p.shortTP3, p.shortLabel, cfg);
  }

  private void drawProjectedSet(DataSeries s, int x1, int x2, int dir, double entry, double sl, double tp1, double tp2, double tp3,
                                String label, SettingsView cfg) {
    long t1 = timeAtOrProjected(s, x1);
    long t2 = timeAtOrProjected(s, x2);
    Color entryColor = alpha(dir > 0 ? cfg.bullColor : cfg.bearColor, 210);
    Color stopColor = alpha(new Color(244, 67, 54), 165);
    Color targetColor = alpha(new Color(76, 175, 80), 165);
    addLevel(t1, t2, entry, label + " " + formatPrice(entry), entryColor, dashed(1.8f));
    addLevel(t1, t2, sl, "P-SL " + formatPrice(sl), stopColor, dotted(1.2f));
    addLevel(t1, t2, tp1, (cfg.singleTarget ? "P-TP " : "P-TP1 ") + formatPrice(tp1), targetColor, dotted(1.0f));
    if (!cfg.singleTarget) addLevel(t1, t2, tp2, "P-TP2 " + formatPrice(tp2), targetColor, dotted(1.0f));
    if (!cfg.singleTarget) addLevel(t1, t2, tp3, "P-TP3 " + formatPrice(tp3), targetColor, dotted(1.0f));
  }

  private void drawDashboard(DataContext ctx, DataSeries s, SettingsView cfg, int signalIndex, boolean[] longSignals, boolean[] shortSignals,
                             double[] atrRisk, double[] rsi, Stoch stoch, Sar sar, Tilson tilson, Smi smi) {
    if (signalIndex < 0 || signalIndex >= s.size()) return;
    BacktestStats stats = MeridianBacktest.runBacktest(s, cfg, signalIndex, longSignals, shortSignals, atrRisk, cfg.dashboardLookback);
    OptimizerResult opt = cfg.showOptimizer ? optimizer.getResult(ctx, s, cfg, signalIndex) : null;
    int bars = Math.min(cfg.dashboardLookback, signalIndex + 1);
    String signal = longSignals[signalIndex] ? "LONG" : shortSignals[signalIndex] ? "SHORT" : "NEUTRAL";
    Color signalColor = longSignals[signalIndex] ? cfg.bullColor : shortSignals[signalIndex] ? cfg.bearColor : cfg.neutralColor;
    double winRate = stats.trades == 0 ? 0.0 : 100.0 * stats.wins / stats.trades;
    double profitFactor = MeridianBacktest.profitFactor(stats);
    double netProfitFactor = MeridianBacktest.netProfitFactor(stats);
    double recoveryFactor = MeridianBacktest.recoveryFactor(stats);
    boolean optStale = opt != null && opt.valid && opt.cfg != null && !sameTuning(cfg, opt.cfg);
    DashboardRow[] rows = new DashboardRow[40];
    int row = 0;
    if (cfg.dashboardCompact) {
      rows[row++] = headerRow("MF", VERSION, signal, signalColor);
      rows[row++] = dashRow("Sig", signal, enabledCount(cfg) + "F " + (cfg.requireAll ? "ALL" : "ANY"), signalColor);
      rows[row++] = dashRow("Grp", cfg.signalGroup, "Manual".equals(cfg.signalGroup) ? cfg.signalSource : signalGroupRegime(cfg), DashboardFigure.ACCENT);
      rows[row++] = dashRow("BT", bars + "b/" + stats.trades + "t", formatPct(winRate) + " WR", DashboardFigure.TEXT);
      rows[row++] = dashRow("PF", formatRatio(profitFactor), "NPF " + formatRatio(netProfitFactor), ratioColor(profitFactor, 1.0));
      rows[row++] = dashRow("Net", formatSigned(stats.netPoints), "DD " + formatPoints(stats.maxDrawdownPoints), signedColor(stats.netPoints));
      rows[row++] = dashRow("Tgt", targetStats(stats, cfg), "SL " + stats.stops, stats.stops <= stats.wins ? DashboardFigure.GOOD : DashboardFigure.WARN);
      if (opt != null && opt.stats != null) {
        rows[row++] = headerRow("Opt", opt.objective + " " + depthLabel(cfg.optimizerDepth), opt.valid ? opt.candidates + "x" : "low trades", opt.valid ? DashboardFigure.GOOD : DashboardFigure.WARN);
        rows[row++] = dashRow("Rec", parameterSummaryRisk(opt.cfg), optStale ? "APPLY" : "OK", optStale ? DashboardFigure.WARN : DashboardFigure.GOOD);
        rows[row++] = dashRow("Perf", formatSigned(opt.stats.netPoints), "PF " + formatRatio(MeridianBacktest.profitFactor(opt.stats)) + " DD " + formatPoints(opt.stats.maxDrawdownPoints), signedColor(opt.stats.netPoints));
      }
    }
    else {
      rows[row++] = headerRow("Meridian Forge", VERSION, signal, signalColor);
      rows[row++] = dashRow("Signal", signal, enabledCount(cfg) + " filters • " + (cfg.requireAll ? "ALL" : "ANY"), signalColor);
      rows[row++] = dashRow("Signal Group", cfg.signalGroup, "Manual".equals(cfg.signalGroup) ? cfg.signalSource : signalGroupRegime(cfg), DashboardFigure.ACCENT);
      rows[row++] = dashRow("Core filters", coreFilterSnapshot(cfg, signalIndex, s.getClose(signalIndex), rsi, stoch, sar), "", DashboardFigure.TEXT);
      rows[row++] = dashRow("Strategy filters", strategyFilterSnapshot(cfg, signalIndex, tilson, smi), "", DashboardFigure.TEXT);
      rows[row++] = dashRow("Target mode", cfg.singleTarget ? "Single target" : "Three targets", parameterSummaryRisk(cfg), DashboardFigure.TEXT);
      rows[row++] = dashRow("Backtest", bars + " bars", stats.trades + " trades", DashboardFigure.TEXT);
      rows[row++] = dashRow("Win rate", formatPct(winRate), stats.wins + "/" + stats.losses + "/" + stats.breakEvens + " W/L/BE", winRate >= 50.0 ? DashboardFigure.GOOD : DashboardFigure.WARN);
      rows[row++] = dashRow("Profit factor", formatRatio(profitFactor), "NPF " + formatRatio(netProfitFactor), ratioColor(profitFactor, 1.0));
      rows[row++] = dashRow("Net points", formatSigned(stats.netPoints), "DD " + formatPoints(stats.maxDrawdownPoints) + " • RF " + formatRatio(recoveryFactor), signedColor(stats.netPoints));
      rows[row++] = dashRow("Gross W/L", formatSigned(stats.grossWinPoints), formatSigned(stats.grossLossPoints), signedColor(stats.grossWinPoints + stats.grossLossPoints));
      rows[row++] = dashRow("Stops / targets", stats.stops + " stops", targetStats(stats, cfg), stats.stops <= stats.wins ? DashboardFigure.GOOD : DashboardFigure.WARN);
      if (opt != null && opt.stats != null) {
        rows[row++] = headerRow("Optimizer", opt.objective + " · " + depthLabel(cfg.optimizerDepth), opt.valid ? opt.candidates + " tries" : "below min trades", opt.valid ? DashboardFigure.GOOD : DashboardFigure.WARN);
        rows[row++] = dashRow("Apply status", optStale ? "REC OUT OF DATE" : "Current matches rec", optStale ? "tick Apply Optimizer" : "", optStale ? DashboardFigure.WARN : DashboardFigure.GOOD);
        rows[row++] = dashRow("Opt stats", formatSigned(opt.stats.netPoints), "PF " + formatRatio(MeridianBacktest.profitFactor(opt.stats)) + " • DD " + formatPoints(opt.stats.maxDrawdownPoints) + " • RF " + formatRatio(MeridianBacktest.recoveryFactor(opt.stats)), signedColor(opt.stats.netPoints));
        rows[row++] = dashRow("Opt core", parameterSummaryCore(opt.cfg), opt.note == null ? "" : opt.note, DashboardFigure.ACCENT);
        rows[row++] = dashRow("Opt risk", parameterSummaryRisk(opt.cfg), "", DashboardFigure.ACCENT);
        String filters = parameterSummaryFilters(opt.cfg, cfg.dashboardHideUnused);
        if (!filters.isEmpty()) rows[row++] = dashRow("Opt filters", filters, "", DashboardFigure.ACCENT);
      }
    }
    if (stats.activeDir != 0) {
      rows[row++] = dashRow(cfg.dashboardCompact ? "Open" : "Open trade", stats.activeDir > 0 ? "LONG " + formatPrice(stats.activeEntry) : "SHORT " + formatPrice(stats.activeEntry),
        "UPL " + formatSigned(stats.activeUnrealized), signedColor(stats.activeUnrealized));
    }
    lastDashboardFigure = new DashboardFigure(rows, row, cfg.dashboardCompact, cfg.dashboardPosPreset, cfg.dashboardXOffset, cfg.dashboardYOffset, cfg.dashboardScale / 100.0, optStale);
    addFigure(lastDashboardFigure);
  }

  private static DashboardRow headerRow(String label, String value, String extra, Color color) {
    return new DashboardRow(label, value, extra, color, true);
  }

  private static DashboardRow dashRow(String label, String value, String extra, Color color) {
    return new DashboardRow(label, value, extra, color, false);
  }

  private String calculationKey(DataSeries s, SettingsView cfg, int signalIndex) {
    StringBuilder b = new StringBuilder(768);
    b.append(MeridianOptimizer.optimizerKey(s, cfg, signalIndex));
    b.append('|').append(s.size() == 0 ? 0L : s.getStartTime(0));
    b.append('|').append(cfg.showStruct).append('|').append(cfg.showBos).append('|').append(cfg.showOB)
      .append('|').append(cfg.obFrom).append('|').append(cfg.obMitWick).append('|').append(cfg.removeMitigated)
      .append('|').append(cfg.obLookback).append('|').append(cfg.maxOB).append('|').append(cfg.obAlpha)
      .append('|').append(cfg.obMean).append('|').append(cfg.obLabels);
    b.append('|').append(cfg.showRisk).append('|').append(cfg.showAtrTrend).append('|').append(cfg.atrTrendLen)
      .append('|').append(cfg.atrTrendMult).append('|').append(cfg.atrSmooth);
    b.append('|').append(cfg.showDashboard).append('|').append(cfg.dashboardLookback).append('|').append(cfg.dashboardMode).append('|').append(cfg.dashboardHideUnused)
      .append('|').append(cfg.dashboardPosPreset).append('|').append(cfg.dashboardXOffset).append('|').append(cfg.dashboardYOffset).append('|').append(cfg.dashboardScale)
      .append('|').append(cfg.showProjection).append('|').append(cfg.projectionBars)
      .append('|').append(cfg.showOptimizer).append('|').append(cfg.optRefreshMode).append('|').append(cfg.optRefreshInterval)
      .append('|').append(cfg.alertSl).append('|').append(cfg.alertTp).append('|').append(cfg.alertOb);
    b.append('|').append(rgb(cfg.bullColor)).append('|').append(rgb(cfg.bearColor))
      .append('|').append(rgb(cfg.obBullColor)).append('|').append(rgb(cfg.obBearColor)).append('|').append(rgb(cfg.neutralColor));
    b.append('|').append(markerSig(UP_MARKER)).append('|').append(markerSig(DOWN_MARKER));
    b.append('|').append(pathSig(STRUCT_PATH)).append('|').append(pathSig(RISK_PATH));
    return b.toString();
  }

  private static int rgb(Color c) {
    return c == null ? 0 : c.getRGB();
  }

  private static int enabledCount(SettingsView cfg) {
    int count = 0;
    if (cfg.enableSma) count++;
    if (cfg.enableRsi) count++;
    if (cfg.enableMacd) count++;
    if (cfg.enableSt) count++;
    if (cfg.enableStoch) count++;
    if (cfg.enableBb) count++;
    if (cfg.enableEma) count++;
    if (cfg.enableAo) count++;
    if (cfg.enableSar) count++;
    if (cfg.enableCci) count++;
    if (cfg.enableAdx) count++;
    if (cfg.enableTilson) count++;
    if (cfg.enableSmi) count++;
    return count;
  }

  private void addLevel(long t1, long t2, double level, String text, Color color, BasicStroke stroke) {
    if (Double.isNaN(level)) return;
    Line line = new Line(t1, level, t2, level);
    line.setColor(color);
    line.setStroke(stroke);
    line.setText(text, new Font(Font.SANS_SERIF, Font.PLAIN, 10));
    if (line.getText() != null) line.getText().setTextColor(color);
    addFigure(line);
  }

  private static List<NVP> opts(String... values) {
    List<NVP> out = new ArrayList<>();
    for (String v : values) out.add(new NVP(v, v));
    return out;
  }

  private static void addLimited(List<FigureEvent> list, int max, FigureEvent e) {
    list.add(e);
    while (list.size() > max) list.remove(0);
  }

  private void signal(DataContext ctx, int index, Signals sig, String msg, double value) {
    if (ctx.isLoadingData()) return;
    ctx.signal(index, sig, msg, value);
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

  private static int findObBar(DataSeries s, int bar, boolean wantBearish, int lookback) {
    int found = -1;
    int fallback = -1;
    for (int back = 1; back <= lookback; back++) {
      int cand = bar - back;
      int next = bar - back + 1;
      if (cand <= 0 || next < 0 || next >= s.size()) break;
      boolean opposite = wantBearish ? s.getClose(cand) < s.getOpen(cand) : s.getClose(cand) > s.getOpen(cand);
      if (!opposite) continue;
      if (fallback < 0) fallback = cand;
      boolean displacement = wantBearish ? s.getClose(next) > s.getHigh(cand) : s.getClose(next) < s.getLow(cand);
      if (displacement) {
        found = cand;
        break;
      }
    }
    return found >= 0 ? found : fallback;
  }

  private static void spawnOB(DataSeries s, List<OBZone> zones, int dir, int obBar, int created, SettingsView cfg) {
    OBZone ob = new OBZone();
    ob.top = s.getHigh(obBar);
    ob.bot = s.getLow(obBar);
    ob.dir = dir;
    ob.left = obBar;
    ob.created = created;
    ob.right = Math.min(s.size() - 1, created + OB_EXTEND_BARS);
    ob.mitigated = false;
    ob.armed = dir == 1 ? s.getClose(created) > ob.top : s.getClose(created) < ob.bot;
    zones.add(ob);
    while (zones.size() > cfg.maxOB) {
      int drop = 0;
      for (int i = 0; i < zones.size(); i++) {
        if (zones.get(i).mitigated) {
          drop = i;
          break;
        }
      }
      zones.remove(drop);
    }
  }

  private void maintainOBs(DataContext ctx, DataSeries s, List<OBZone> zones, int i, SettingsView cfg, int signalIndex) {
    for (int z = zones.size() - 1; z >= 0; z--) {
      OBZone ob = zones.get(z);
      if (!ob.mitigated) {
        if (!ob.armed) ob.armed = ob.dir == 1 ? s.getClose(i) > ob.top : s.getClose(i) < ob.bot;
        boolean mit = ob.armed && s.isBarComplete(i) && i > ob.created
          && (ob.dir == 1
            ? (cfg.obMitWick ? s.getLow(i) <= ob.top : s.getClose(i) <= ob.top)
            : (cfg.obMitWick ? s.getHigh(i) >= ob.bot : s.getClose(i) >= ob.bot));
        if (mit) {
          ob.mitigated = true;
          ob.right = i;
          if (i == signalIndex && cfg.alertOb) {
            signal(ctx, i, Signals.OB_MITIGATED, (ob.dir == 1 ? "Bull" : "Bear") + " OB mitigated", ob.dir == 1 ? ob.top : ob.bot);
          }
          if (cfg.removeMitigated) zones.remove(z);
        }
        else {
          ob.right = Math.min(s.size() - 1, i + OB_EXTEND_BARS);
        }
      }
    }
  }

  private static ForgeState forgeState(SettingsView c, int i, double[] smaFast, double[] smaSlow, double[] emaFast,
                                       double[] emaSlow, double[] rsi, Macd macd, Stoch stoch, Bands bb,
                                       double[] closes, double[] ao, Sar sar, double[] cci, Adx adx, Super st,
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
    if (c.enableCci) { any = true; longCond = merge(c.requireAll, longCond, cci[i] > c.cciLong); shortCond = merge(c.requireAll, shortCond, cci[i] < c.cciShort); }
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

  private static boolean targetHitBeforeStop(boolean slHit, boolean targetHit) {
    return targetHit && !slHit;
  }

  private static boolean merge(boolean requireAll, boolean current, boolean next) {
    return requireAll ? current && next : current || next;
  }

  private HtfBias buildHtfBias(SettingsView cfg, DataContext ctx, DataSeries base, int n) {

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
  private static TradeLines snapshot(int dir, int entryIndex, int endIndex, double entry, double sl, double tp1, double tp2, double tp3,
                                     boolean tp1Hit, boolean tp2Hit, boolean tp3Hit, boolean beActive) {
    TradeLines t = new TradeLines();
    t.dir = dir; t.entryIndex = entryIndex; t.endIndex = endIndex; t.entry = entry; t.sl = sl; t.tp1 = tp1; t.tp2 = tp2; t.tp3 = tp3;
    t.tp1Hit = tp1Hit; t.tp2Hit = tp2Hit; t.tp3Hit = tp3Hit; t.beActive = beActive;
    return t;
  }

  private static int latestCompleteIndex(DataSeries s) {
    for (int i = s.size() - 1; i >= 0; i--) {
      if (s.isBarComplete(i)) return i;
    }
    return s.size() - 1;
  }

  private static long timeAtOrProjected(DataSeries s, int index) {
    if (index < s.size()) return s.getStartTime(Math.max(0, index));
    long step = s.getBarSize() == null ? 0 : s.getBarSize().getIntervalMillis();
    if (step <= 0 && s.size() > 1) step = s.getStartTime(s.size() - 1) - s.getStartTime(s.size() - 2);
    if (step <= 0) step = 60_000L;
    return s.getStartTime(s.size() - 1) + (index - (s.size() - 1L)) * step;
  }

  private static Color alpha(Color c, int a) {
    int alpha = Math.max(0, Math.min(255, a));
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
  }

  private static BasicStroke solid(float width) {
    return new BasicStroke(width);
  }

  private static BasicStroke dashed(float width) {
    return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[] {8f, 5f}, 0.0f);
  }

  private static BasicStroke dotted(float width) {
    return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10.0f, new float[] {1f, 5f}, 0.0f);
  }

  private static Color structColor(PathInfo p) {
    Color c = p == null ? null : p.getColor();
    return c == null ? new Color(128, 128, 128) : c;
  }

  private static BasicStroke structStroke(PathInfo p) {
    if (p == null) return new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[] {3f, 3f}, 0.0f);
    float width = p.getStrokeWidth();
    float[] dash = p.getDash();
    return dash == null ? new BasicStroke(width) : new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f);
  }

  private static Color riskColor(PathInfo p) {
    Color c = p == null ? null : p.getColor();
    return c == null ? new Color(80, 140, 180) : c;
  }

  private static BasicStroke riskStroke(PathInfo p) {
    if (p == null) return dotted(1.0f);
    float width = p.getStrokeWidth();
    float[] dash = p.getDash();
    return dash == null ? new BasicStroke(width) : new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, dash, 0.0f);
  }

  private static float riskWidth(PathInfo p) {
    return p == null ? 1.0f : p.getStrokeWidth();
  }

  private String markerSig(String id) {
    MarkerInfo m = getSettings().getMarker(id);
    return m == null ? "null" : m.getKey() + ":" + m.isEnabled();
  }

  private String pathSig(String id) {
    PathInfo p = getSettings().getPath(id);
    if (p == null) return "null";
    return p.getKey() + ":" + p.isEnabled() + ":" + rgb(p.getColor()) + ":" + p.getStrokeWidth() + ":" + dashSig(p.getDash());
  }

  private static String dashSig(float[] dash) {
    if (dash == null) return "S";
    StringBuilder sb = new StringBuilder();
    for (float f : dash) sb.append((int) (f * 100));
    return sb.toString();
  }

}
