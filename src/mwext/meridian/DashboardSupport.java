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
    String group = "Manual".equals(cfg.signalGroup) ? cfg.signalSource : cfg.signalGroup;
    return "Sw " + cfg.swingLen + " • " + group + " • " + cfg.signalMode;
  }

  static String signalGroupRegime(SettingsView cfg) {
    return switch (cfg.signalGroup) {
      case "Trend Confirmation" -> "Trending";
      case "Momentum Pullback" -> "Trend w/ Pullback";
      case "Mean Reversion" -> "Ranging / Mean-Reverting";
      case "Structure Only" -> "Structure / No Filter";
      case "Balanced" -> "Balanced Multi-Filter";
      default -> cfg.signalSource;
    };
  }

  static String depthLabel(String depth) {
    return switch (depth) {
      case "Deep" -> "Deep";
      case "Medium" -> "Med";
      default -> "Fast";
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
    return a.swingLen == b.swingLen && a.breakOnWick == b.breakOnWick
      && a.atrRiskLen == b.atrRiskLen && a.useBreakEven == b.useBreakEven
      && a.useHtf == b.useHtf && eq(a.htfBarSize, b.htfBarSize) && a.htfEmaLen == b.htfEmaLen
      && a.requireAll == b.requireAll
      && a.enableSma == b.enableSma && a.smaFast == b.smaFast && a.smaSlow == b.smaSlow
      && a.enableRsi == b.enableRsi && a.rsiLen == b.rsiLen
      && near(a.rsiLong, b.rsiLong) && near(a.rsiShort, b.rsiShort)
      && a.enableMacd == b.enableMacd && a.macdFast == b.macdFast && a.macdSlow == b.macdSlow
      && a.macdSignal == b.macdSignal
      && a.enableSt == b.enableSt && a.stLen == b.stLen && near(a.stFactor, b.stFactor)
      && a.enableStoch == b.enableStoch && a.stochK == b.stochK && a.stochD == b.stochD
      && a.stochSmooth == b.stochSmooth
      && a.enableBb == b.enableBb && a.bbLen == b.bbLen && near(a.bbMult, b.bbMult)
      && a.enableEma == b.enableEma && a.emaFast == b.emaFast && a.emaSlow == b.emaSlow
      && a.enableAo == b.enableAo
      && a.enableSar == b.enableSar && near(a.sarStart, b.sarStart) && near(a.sarInc, b.sarInc)
      && near(a.sarMax, b.sarMax)
      && a.enableCci == b.enableCci && a.cciLen == b.cciLen
      && near(a.cciLong, b.cciLong) && near(a.cciShort, b.cciShort)
      && a.enableAdx == b.enableAdx && a.diLen == b.diLen && a.adxLen == b.adxLen
      && near(a.adxThreshold, b.adxThreshold)
      && a.enableTilson == b.enableTilson && a.tilsonPeriod == b.tilsonPeriod
      && a.enableSmi == b.enableSmi && a.smiLongPeriod == b.smiLongPeriod
      && a.smiShortPeriod == b.smiShortPeriod && a.smiSignalPeriod == b.smiSignalPeriod
      && near(a.smiTopGuide, b.smiTopGuide) && near(a.smiBottomGuide, b.smiBottomGuide)
      && near(a.slMultEff, b.slMultEff) && near(a.tpEff, b.tpEff) && near(a.tp1Eff, b.tp1Eff)
      && near(a.tp2Eff, b.tp2Eff) && near(a.tp3Eff, b.tp3Eff) && a.singleTarget == b.singleTarget
      && eq(a.signalMode, b.signalMode) && eq(a.signalSource, b.signalSource)
      && eq(a.riskPreset, b.riskPreset) && eq(a.tpMode, b.tpMode)
      && eq(a.tilsonInput, b.tilsonInput) && eq(a.tilsonMethod, b.tilsonMethod)
      && eq(a.smiInput, b.smiInput) && eq(a.smiMethod, b.smiMethod) && eq(a.smiMode, b.smiMode);
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

  private static boolean near(double a, double b) {
    return Math.abs(a - b) < 0.0000001;
  }

  private static boolean eq(Object a, Object b) {
    return a == null ? b == null : a.equals(b);
  }

  private static String side(boolean longOk, boolean shortOk) {
    return longOk ? " L" : shortOk ? " S" : " -";
  }
}
