package mwext.meridian;

final class MeridianOptions {
  private MeridianOptions() {}

  static final String CLOSE = "Close";
  static final String OPEN = "Open";
  static final String HIGH = "High";
  static final String LOW = "Low";
  static final String HL2 = "HL2";
  static final String HLC3 = "HLC3";
  static final String WICK = "Wick";
  static final String SMA = "SMA";
  static final String EMA = "EMA";
  static final String MEMA = "MEMA";
  static final String BALANCED = "Balanced";
  static final String NO_FILTERING = "No Filtering";
  static final String FAST = "Fast";
  static final String MEDIUM = "Medium";
  static final String DEEP = "Deep";
  static final String SLOW = "Slow";
  static final String FULL = "Full";
  static final String COMPACT = "Compact";
  static final String NQ_5_15M_FAST = "NQ 5/15m Fast";
  static final String AROUND_CURRENT = "Around Current";
  static final String ON_DEMAND = "On Demand";
  static final String EVERY_N_BARS = "Every N Bars";
  static final String EVERY_BAR = "Every Bar";
  static final String NET_POINTS = "Net Points";
  static final String PROFIT_FACTOR = "Profit Factor";
  static final String PF_VS_MAX_DD = "PF vs Max DD";
  static final String RECOVERY_FACTOR = "Recovery Factor";

  static final String[] PRICE_SOURCES = { CLOSE, OPEN, HIGH, LOW, HL2, HLC3 };
  static final String[] MA_METHODS = { SMA, EMA, MEMA };
}

enum SignalModeOption {
  BOS_CHOCH("BOS + CHoCH"),
  CHOCH_ONLY("CHoCH only"),
  BOS_ONLY("BOS only");

  final String label;

  SignalModeOption(String label) {
    this.label = label;
  }

  boolean allows(boolean bos, boolean choch) {
    return switch (this) {
      case CHOCH_ONLY -> choch;
      case BOS_ONLY -> bos;
      default -> bos || choch;
    };
  }

  static boolean eventAllowed(String label, boolean bos, boolean choch) {
    return from(label).allows(bos, choch);
  }

  static SignalModeOption from(String label) {
    for (SignalModeOption option : values()) {
      if (option.label.equals(label)) return option;
    }
    return BOS_CHOCH;
  }

  static String[] labels() {
    SignalModeOption[] values = values();
    String[] out = new String[values.length];
    for (int i = 0; i < values.length; i++) out[i] = values[i].label;
    return out;
  }
}

enum SignalSourceOption {
  STRUCTURE_FORGE("Structure + Forge"),
  STRUCTURE_ONLY("Structure only"),
  FORGE_ONLY("Forge only");

  final String label;

  SignalSourceOption(String label) {
    this.label = label;
  }

  boolean usesStructure() {
    return this != FORGE_ONLY;
  }

  boolean usesForge() {
    return this != STRUCTURE_ONLY;
  }

  static SignalSourceOption from(String label) {
    for (SignalSourceOption option : values()) {
      if (option.label.equals(label)) return option;
    }
    return STRUCTURE_FORGE;
  }

  static String[] labels() {
    SignalSourceOption[] values = values();
    String[] out = new String[values.length];
    for (int i = 0; i < values.length; i++) out[i] = values[i].label;
    return out;
  }
}

enum RiskPresetOption {
  CONSERVATIVE("Conservative", 2.5, 2.0, 1.0, 2.0, 4.0),
  BALANCED(MeridianOptions.BALANCED, 1.5, 2.0, 1.0, 2.0, 3.0),
  AGGRESSIVE("Aggressive", 1.0, 2.5, 1.5, 2.5, 4.0),
  SCALPING("Scalping", 0.8, 1.5, 0.8, 1.5, 2.0),
  // CUSTOM reads raw multiplier fields from SettingsView instead of these sentinel values.
  CUSTOM("Custom", Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);

  final String label;
  final double sl;
  final double tp;
  final double tp1;
  final double tp2;
  final double tp3;

  RiskPresetOption(String label, double sl, double tp, double tp1, double tp2, double tp3) {
    this.label = label;
    this.sl = sl;
    this.tp = tp;
    this.tp1 = tp1;
    this.tp2 = tp2;
    this.tp3 = tp3;
  }

  void apply(SettingsView cfg) {
    if (this == CUSTOM) {
      cfg.slMultEff = cfg.slMultRaw;
      cfg.tpEff = cfg.tpMultRaw;
      cfg.tp1Eff = cfg.tp1MultRaw;
      cfg.tp2Eff = cfg.tp2MultRaw;
      cfg.tp3Eff = cfg.tp3MultRaw;
      return;
    }
    cfg.slMultEff = sl;
    cfg.tpEff = tp;
    cfg.tp1Eff = tp1;
    cfg.tp2Eff = tp2;
    cfg.tp3Eff = tp3;
  }

  static RiskPresetOption from(String label) {
    for (RiskPresetOption option : values()) {
      if (option.label.equals(label)) return option;
    }
    return BALANCED;
  }

  static String[] labels() {
    RiskPresetOption[] values = values();
    String[] out = new String[values.length];
    for (int i = 0; i < values.length; i++) out[i] = values[i].label;
    return out;
  }
}

enum TakeProfitModeOption {
  SINGLE("Single Target"),
  THREE("Three Targets");

  final String label;

  TakeProfitModeOption(String label) {
    this.label = label;
  }

  static boolean isSingle(String label) {
    return SINGLE.label.equals(label);
  }

  static String[] labels() {
    TakeProfitModeOption[] values = values();
    String[] out = new String[values.length];
    for (int i = 0; i < values.length; i++) out[i] = values[i].label;
    return out;
  }
}

enum SmiModeOption {
  LINE_VS_SIGNAL("Line vs Signal"),
  ZERO_BIAS("Zero Bias"),
  GUIDED_REVERSAL("Guided Reversal");

  final String label;

  SmiModeOption(String label) {
    this.label = label;
  }

  static String[] labels() {
    SmiModeOption[] values = values();
    String[] out = new String[values.length];
    for (int i = 0; i < values.length; i++) out[i] = values[i].label;
    return out;
  }
}

enum OptimizerDepthOption {
  FAST(MeridianOptions.FAST),
  MEDIUM(MeridianOptions.MEDIUM),
  DEEP(MeridianOptions.DEEP);

  final String label;

  OptimizerDepthOption(String label) {
    this.label = label;
  }

  static String[] labels() {
    OptimizerDepthOption[] values = values();
    String[] out = new String[values.length];
    for (int i = 0; i < values.length; i++) out[i] = values[i].label;
    return out;
  }
}

enum OptimizerRefreshOption {
  ON_DEMAND(MeridianOptions.ON_DEMAND),
  EVERY_N_BARS(MeridianOptions.EVERY_N_BARS),
  EVERY_BAR(MeridianOptions.EVERY_BAR);

  final String label;

  OptimizerRefreshOption(String label) {
    this.label = label;
  }

  static String[] labels() {
    OptimizerRefreshOption[] values = values();
    String[] out = new String[values.length];
    for (int i = 0; i < values.length; i++) out[i] = values[i].label;
    return out;
  }
}
