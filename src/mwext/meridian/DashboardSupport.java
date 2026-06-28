package mwext.meridian;

import com.motivewave.platform.sdk.common.Util;

final class DashboardSupport {
  private DashboardSupport() {}

  static java.awt.Color signedColor(double value) {
    if (Double.isNaN(value)) return DashboardFigure.TEXT;
    return value > 0.0 ? DashboardFigure.GOOD : value < 0.0 ? DashboardFigure.BAD : DashboardFigure.TEXT;
  }

  static java.awt.Color ratioColor(double value, double neutral) {
    if (Double.isNaN(value)) return DashboardFigure.TEXT;
    return value > neutral ? DashboardFigure.GOOD : value < neutral ? DashboardFigure.BAD : DashboardFigure.WARN;
  }

  static String parameterSummary(SettingsView cfg) {
    String filters = parameterSummaryFilters(cfg, true);
    return parameterSummaryCore(cfg) + " | " + parameterSummaryRisk(cfg) + (filters.isEmpty() ? "" : " | " + filters);
  }

  static String parameterSummaryCore(SettingsView cfg) {
    return "Sw " + cfg.swingLen + " • " + cfg.signalSource + " • " + cfg.signalMode;
  }


  static String depthLabel(String depth) {
    return switch (depth) {
      case MeridianOptions.DEEP -> MeridianOptions.DEEP;
      case MeridianOptions.MEDIUM -> "Med";
      default -> MeridianOptions.FAST;
    };
  }

  static String parameterSummaryRisk(SettingsView cfg) {
    if (cfg.singleTarget) {
      return "ATR " + cfg.atrRiskLen + " SLx" + formatOne(cfg.slMultEff) + " TPx" + formatOne(cfg.tpEff);
    }
    return "ATR " + cfg.atrRiskLen + " SLx" + formatOne(cfg.slMultEff) + " TPx"
      + formatOne(cfg.tp1Eff) + "/" + formatOne(cfg.tp2Eff) + "/" + formatOne(cfg.tp3Eff);
  }

  static String parameterSummaryFilters(SettingsView cfg, boolean hideUnused) {
    StringBuilder b = new StringBuilder(128);
    appendPart(b, cfg.enableSma ? "SMA " + cfg.smaFast + "/" + cfg.smaSlow : hideUnused ? "" : "SMA off");
    appendPart(b, cfg.enableRsi ? "RSI " + cfg.rsiLen + " " + formatOne(cfg.rsiLong) + "/" + formatOne(cfg.rsiShort) : hideUnused ? "" : "RSI off");
    appendPart(b, cfg.enableStoch ? "STO " + cfg.stochK + "/" + cfg.stochD + "/" + cfg.stochSmooth : hideUnused ? "" : "STO off");
    appendPart(b, cfg.enableSar ? "SAR " + formatOne(cfg.sarStart) + "/" + formatOne(cfg.sarInc) + "/" + formatOne(cfg.sarMax) : hideUnused ? "" : "SAR off");
    appendPart(b, cfg.enableTilson ? "IE2 " + cfg.tilsonInput + "/" + cfg.tilsonMethod + "/" + cfg.tilsonPeriod : hideUnused ? "" : "IE2 off");
    appendPart(b, cfg.enableSmi ? "SMI " + cfg.smiInput + "/" + cfg.smiMethod + "/" + cfg.smiLongPeriod + "/" + cfg.smiShortPeriod + "/" + cfg.smiSignalPeriod : hideUnused ? "" : "SMI off");
    return b.toString();
  }

  static void appendPart(StringBuilder b, String part) {
    if (part == null || part.isEmpty()) return;
    if (b.length() > 0) b.append(" | ");
    b.append(part);
  }

  static String targetMessage(SettingsView cfg, double tp1, double tp2, double tp3) {
    if (cfg.singleTarget) return " | TP " + formatPrice(tp1);
    return " | TP1 " + formatPrice(tp1) + " | TP2 " + formatPrice(tp2) + " | TP3 " + formatPrice(tp3);
  }

  static String targetStats(BacktestStats stats, SettingsView cfg) {
    return cfg.singleTarget ? "TP " + stats.tp1Hits : "TP " + stats.tp1Hits + "/" + stats.tp2Hits + "/" + stats.tp3Hits;
  }

  static boolean sameTuning(SettingsView a, SettingsView b) {
    if (a == null || b == null) return false;
    return a.tuningKey().equals(b.tuningKey());
  }

  static String stateLabel(boolean longSignal, boolean shortSignal, Projection projection,
                           int structTrend, double breakHighSrc, double breakLowSrc,
                           double lastSwingHigh, double lastSwingLow,
                           boolean swingHighBroken, boolean swingLowBroken,
                           boolean forgeLongNow, boolean forgeShortNow,
                           boolean htfLongNow, boolean htfShortNow,
                           boolean usesStructure, boolean usesForge) {
    if (longSignal && !shortSignal) return "LONG SIGNAL";
    if (shortSignal && !longSignal) return "SHORT SIGNAL";

    boolean liveBullBreak = usesStructure && !Double.isNaN(lastSwingHigh) && !swingHighBroken && breakHighSrc > lastSwingHigh && htfLongNow;
    boolean liveBearBreak = usesStructure && !Double.isNaN(lastSwingLow) && !swingLowBroken && breakLowSrc < lastSwingLow && htfShortNow;
    boolean longFilterOk = !usesForge || forgeLongNow;
    boolean shortFilterOk = !usesForge || forgeShortNow;
    if (liveBullBreak && !liveBearBreak) return longFilterOk ? "LIVE LONG" : "LONG BREAK";
    if (liveBearBreak && !liveBullBreak) return shortFilterOk ? "LIVE SHORT" : "SHORT BREAK";

    if (projection != null) {
      if (projection.longValid && !projection.shortValid) {
        String compact = compactProjectionLabel(projection.longLabel, true);
        if (!compact.startsWith("WAIT")) return compact;
      }
      if (projection.shortValid && !projection.longValid) {
        String compact = compactProjectionLabel(projection.shortLabel, false);
        if (!compact.startsWith("WAIT")) return compact;
      }

      boolean longReady = projectionReady(projection.longLabel);
      boolean shortReady = projectionReady(projection.shortLabel);
      if (longReady && !shortReady) return "NEXT LONG";
      if (shortReady && !longReady) return "NEXT SHORT";
      if (longReady && shortReady) return "DUAL SETUP";
    }

    if (usesStructure && structTrend > 0 && htfLongNow && longFilterOk) return "LONG BIAS";
    if (usesStructure && structTrend < 0 && htfShortNow && shortFilterOk) return "SHORT BIAS";
    if (!usesStructure && usesForge && forgeLongNow && !forgeShortNow && htfLongNow) return "FORGE LONG";
    if (!usesStructure && usesForge && forgeShortNow && !forgeLongNow && htfShortNow) return "FORGE SHORT";
    if (usesStructure && structTrend > 0) return "BULL STRUCT";
    if (usesStructure && structTrend < 0) return "BEAR STRUCT";

    if (projection != null) {
      if (projection.longValid && !projection.shortValid) return compactProjectionLabel(projection.longLabel, true);
      if (projection.shortValid && !projection.longValid) return compactProjectionLabel(projection.shortLabel, false);
      if (projection.longValid && projection.shortValid) return "RANGE WAIT";
    }
    return "NEUTRAL";
  }

  static java.awt.Color stateColor(String state, SettingsView cfg) {
    if (state == null) return cfg.neutralColor;
    if (state.contains("LONG")) return cfg.bullColor;
    if (state.contains("SHORT")) return cfg.bearColor;
    if ("DUAL SETUP".equals(state) || "RANGE WAIT".equals(state)) return DashboardFigure.ACCENT;
    return cfg.neutralColor;
  }

  static String coreFilterSnapshot(SettingsView cfg, int i, double close, double[] rsi, Stoch stoch, Sar sar) {
    StringBuilder b = new StringBuilder(96);
    if (cfg.enableRsi && rsi != null && i < rsi.length) {
      double v = rsi[i];
      appendPart(b, "RSI " + formatOne(v) + side(v > cfg.rsiLong, v < cfg.rsiShort));
    }
    else if (!cfg.dashboardHideUnused) appendPart(b, cfg.enableRsi ? "RSI n/a" : "RSI off");
    if (cfg.enableStoch && stoch != null && stoch.k != null && stoch.d != null && i < stoch.k.length && i < stoch.d.length) {
      double k = stoch.k[i];
      double d = stoch.d[i];
      appendPart(b, "STO " + formatOne(k) + "/" + formatOne(d)
        + side(!Double.isNaN(k) && !Double.isNaN(d) && k > d && k > 50,
               !Double.isNaN(k) && !Double.isNaN(d) && k < d && k < 50));
    }
    else if (!cfg.dashboardHideUnused) appendPart(b, cfg.enableStoch ? "STO n/a" : "STO off");
    if (cfg.enableSar && sar != null && sar.value != null && i < sar.value.length) {
      double v = sar.value[i];
      appendPart(b, "SAR " + formatPrice(v) + side(close > v, close < v));
    }
    else if (!cfg.dashboardHideUnused) appendPart(b, cfg.enableSar ? "SAR n/a" : "SAR off");
    return b.length() == 0 ? "none" : b.toString();
  }

  static String strategyFilterSnapshot(SettingsView cfg, int i, Tilson tilson, Smi smi) {
    StringBuilder b = new StringBuilder(96);
    if (cfg.enableTilson && tilson != null) {
      double v = tilson.value[i];
      double p = tilson.price[i];
      appendPart(b, "IE2 " + formatPrice(v) + side(!Double.isNaN(v) && !Double.isNaN(p) && v > p,
        !Double.isNaN(v) && !Double.isNaN(p) && v < p));
    }
    else if (!cfg.dashboardHideUnused) appendPart(b, "IE2 off");
    if (cfg.enableSmi && smi != null) {
      double v = smi.value[i];
      double sig = smi.signal[i];
      appendPart(b, "SMI " + formatOne(v) + "/" + formatOne(sig)
        + side(!Double.isNaN(v) && !Double.isNaN(sig) && v > sig,
               !Double.isNaN(v) && !Double.isNaN(sig) && v < sig));
    }
    else if (!cfg.dashboardHideUnused) appendPart(b, "SMI off");
    return b.length() == 0 ? "none" : b.toString();
  }

  static String formatSigned(double v) {
    if (Double.isNaN(v)) return "n/a";
    String s = Util.formatDouble(v, 2);
    return v > 0 ? "+" + s : s;
  }

  static String formatPct(double v) {
    if (Double.isNaN(v)) return "n/a";
    return Util.formatDouble(v, 1) + "%";
  }

  static String formatPoints(double v) {
    if (Double.isNaN(v)) return "n/a";
    return Util.formatDouble(Math.abs(v), 2);
  }

  static String formatRatio(double v) {
    if (Double.isNaN(v)) return "n/a";
    if (Double.isInfinite(v)) return "∞";
    return Util.formatDouble(v, 2);
  }

  static String formatOne(double v) {
    if (Double.isNaN(v)) return "n/a";
    return Util.formatDouble(v, 1);
  }

  static String formatPrice(double v) {
    if (Double.isNaN(v)) return "n/a";
    return Util.formatDouble(v, 2);
  }

  private static String side(boolean longOk, boolean shortOk) {
    return longOk ? " L" : shortOk ? " S" : " -";
  }

  private static String compactProjectionLabel(String label, boolean isLong) {
    if (label == null || label.isEmpty()) return isLong ? "NEXT LONG" : "NEXT SHORT";
    if (label.startsWith("WAIT")) return isLong ? "WAIT LONG" : "WAIT SHORT";
    return isLong ? "NEXT LONG" : "NEXT SHORT";
  }

  private static boolean projectionReady(String label) {
    return label != null && !label.startsWith("WAIT");
  }
}
