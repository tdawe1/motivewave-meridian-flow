package mwext.meridian;

import com.motivewave.platform.sdk.common.DataSeries;

import static mwext.meridian.MeridianIndicators.*;

final class TradingBotEngine {
  private TradingBotEngine() {}

  static TradingBotResult calculate(DataSeries s, SettingsView cfg) {
    int n = s == null ? 0 : s.size();
    TradingBotResult out = new TradingBotResult(n);
    if (n == 0 || cfg == null) return out;

    double[] close = closeArray(s);
    double[] atr100 = atr(s, 100);
    double[] atrRisk = atr(s, cfg.tradingBotAtrLen);
    double[] rsi = rsi(s, cfg.tradingBotRsiLen);

    computeHalfTrend(s, cfg, out, atr100);
    computeBollingerTrend(s, cfg, out, close, atrRisk, rsi);
    computeTsiPivotCurl(s, cfg, out, close);
    computeAdaptiveEmaFlip(close, out);
    mergeSignals(cfg, out);
    return out;
  }

  private static void computeHalfTrend(DataSeries s, SettingsView cfg, TradingBotResult out, double[] atr100) {
    int n = out.longSignal.length;
    int amplitude = Math.max(2, cfg.tradingBotAmplitude);
    double maxLowPrice = n > 1 ? s.getLow(1) : s.getLow(0);
    double minHighPrice = n > 1 ? s.getHigh(1) : s.getHigh(0);
    double up = Double.NaN;
    double down = Double.NaN;
    int trend = 0;
    int nextTrend = 0;

    for (int i = 0; i < n; i++) {
      double highPrice = highAtHighestBar(s, i, amplitude);
      double lowPrice = lowAtLowestBar(s, i, amplitude);
      double highMa = smaHigh(s, i, amplitude);
      double lowMa = smaLow(s, i, amplitude);
      double prevLow = i > 0 ? s.getLow(i - 1) : s.getLow(i);
      double prevHigh = i > 0 ? s.getHigh(i - 1) : s.getHigh(i);
      int prevTrend = trend;

      if (nextTrend == 1) {
        maxLowPrice = Math.max(lowPrice, maxLowPrice);
        if (!Double.isNaN(highMa) && highMa < maxLowPrice && s.getClose(i) < prevLow) {
          trend = 1;
          nextTrend = 0;
          minHighPrice = highPrice;
        }
      }
      else {
        minHighPrice = Math.min(highPrice, minHighPrice);
        if (!Double.isNaN(lowMa) && lowMa > minHighPrice && s.getClose(i) > prevHigh) {
          trend = 0;
          nextTrend = 1;
          maxLowPrice = lowPrice;
        }
      }

      if (trend == 0) {
        up = prevTrend != 0 && !Double.isNaN(down) ? down : Double.isNaN(up) ? maxLowPrice : Math.max(maxLowPrice, up);
        out.halfTrendLine[i] = up;
      }
      else {
        down = prevTrend != 1 && !Double.isNaN(up) ? up : Double.isNaN(down) ? minHighPrice : Math.min(minHighPrice, down);
        out.halfTrendLine[i] = down;
      }
      out.halfTrendDir[i] = trend == 0 ? 1 : -1;
      out.halfTrendLong[i] = trend == 0 && prevTrend == 1 && i > 0 && nz(atr100[i]) > 0.0;
      out.halfTrendShort[i] = trend == 1 && prevTrend == 0 && i > 0 && nz(atr100[i]) > 0.0;
    }
  }

  private static void computeBollingerTrend(DataSeries s, SettingsView cfg, TradingBotResult out, double[] close, double[] atrRisk, double[] rsi) {
    int n = close.length;
    Bands bands = bollinger(close, cfg.tradingBotBbPeriod, cfg.tradingBotBbDeviation);
    double trendLine = Double.NaN;
    int trend = 0;
    for (int i = 0; i < n; i++) {
      int prevTrend = trend;
      int signal = !Double.isNaN(bands.upper[i]) && close[i] > bands.upper[i] ? 1
        : !Double.isNaN(bands.lower[i]) && close[i] < bands.lower[i] ? -1 : 0;
      double candidate = trendLine;
      if (signal == 1) {
        candidate = s.getLow(i);
        if (!Double.isNaN(trendLine) && candidate < trendLine) candidate = trendLine;
      }
      else if (signal == -1) {
        candidate = s.getHigh(i);
        if (!Double.isNaN(trendLine) && candidate > trendLine) candidate = trendLine;
      }
      if (!Double.isNaN(candidate)) trendLine = candidate;
      if (i > 0 && !Double.isNaN(trendLine) && !Double.isNaN(out.trendLine[i - 1])) {
        if (trendLine > out.trendLine[i - 1]) trend = 1;
        else if (trendLine < out.trendLine[i - 1]) trend = -1;
      }
      out.trendLine[i] = trendLine;
      out.trendDir[i] = trend;
      boolean filterOk = tradingBotFilterOk(cfg, i, atrRisk, rsi);
      out.trendLong[i] = cfg.tradingBotLongSide && filterOk && prevTrend == -1 && trend == 1;
      out.trendShort[i] = cfg.tradingBotShortSide && filterOk && prevTrend == 1 && trend == -1;
    }
  }

  private static void computeTsiPivotCurl(DataSeries s, SettingsView cfg, TradingBotResult out, double[] close) {
    int n = close.length;
    int shortLen = "Slow".equals(cfg.tradingBotTsiSpeed) ? cfg.tradingBotTsiSlowShort : cfg.tradingBotTsiFastShort;
    int longLen = "Slow".equals(cfg.tradingBotTsiSpeed) ? cfg.tradingBotTsiSlowLong : cfg.tradingBotTsiFastLong;
    int signalLen = "Slow".equals(cfg.tradingBotTsiSpeed) ? cfg.tradingBotTsiSlowSignal : cfg.tradingBotTsiFastSignal;
    double[] tsi = tsi(close, shortLen, longLen);
    double[] tsl = emaWithNa(tsi, signalLen, 2.0 / (signalLen + 1.0));
    int swing = Math.max(2, cfg.tradingBotSwing);
    for (int i = 1; i < n; i++) {
      boolean closeAtLow = close[i] == lowest(close, i, swing);
      boolean closeAtHigh = close[i] == highest(close, i, swing);
      boolean bullCurl = !Double.isNaN(tsi[i]) && !Double.isNaN(tsl[i]) && tsi[i] > tsi[i - 1] && tsi[i] < tsl[i];
      boolean bearCurl = !Double.isNaN(tsi[i]) && !Double.isNaN(tsl[i]) && tsi[i] < tsi[i - 1] && tsi[i] > tsl[i];
      out.tsiCurlLong[i] = bullCurl && closeAtLow;
      out.tsiCurlShort[i] = bearCurl && closeAtHigh;
    }
  }

  private static void computeAdaptiveEmaFlip(double[] close, TradingBotResult out) {
    double ema = Double.NaN;
    boolean[] above = new boolean[close.length];
    for (int i = 0; i < close.length; i++) {
      double alpha = 0.1 / (i + 1.0);
      ema = Double.isNaN(ema) ? close[i] : alpha * close[i] + (1.0 - alpha) * ema;
      out.adaptiveEma[i] = ema;
      above[i] = i > 0 && out.adaptiveEma[i - 1] < ema;
      if (i >= 3) {
        out.ademaLong[i] = !above[i - 3] && !above[i - 2] && !above[i - 1] && above[i];
        out.ademaShort[i] = above[i - 3] && above[i - 2] && above[i - 1] && !above[i];
      }
    }
  }

  private static void mergeSignals(SettingsView cfg, TradingBotResult out) {
    for (int i = 0; i < out.longSignal.length; i++) {
      boolean longSig = false;
      boolean shortSig = false;
      if (cfg.tradingBotUseHalfTrend) {
        longSig |= out.halfTrendLong[i];
        shortSig |= out.halfTrendShort[i];
      }
      if (cfg.tradingBotUseTrendLine) {
        longSig |= out.trendLong[i];
        shortSig |= out.trendShort[i];
      }
      if (cfg.tradingBotUseTsiCurl) {
        longSig |= out.tsiCurlLong[i];
        shortSig |= out.tsiCurlShort[i];
      }
      if (cfg.tradingBotUseAdema) {
        longSig |= out.ademaLong[i];
        shortSig |= out.ademaShort[i];
      }
      if (longSig && shortSig) {
        longSig = false;
        shortSig = false;
      }
      out.longSignal[i] = longSig;
      out.shortSignal[i] = shortSig;
    }
  }

  private static boolean tradingBotFilterOk(SettingsView cfg, int i, double[] atr, double[] rsi) {
    boolean atrReady = i > 0 && !Double.isNaN(atr[i]) && !Double.isNaN(atr[i - 1]);
    boolean atrExpanding = atrReady && atr[i] >= atr[i - 1];
    boolean rsiImpulse = rsi != null && i < rsi.length && (rsi[i] > cfg.tradingBotRsiTop || rsi[i] < cfg.tradingBotRsiBot);
    return switch (cfg.tradingBotFilter) {
      case "ATR" -> atrExpanding;
      case "RSI" -> rsiImpulse;
      case "ATR or RSI" -> atrExpanding || rsiImpulse;
      case "ATR and RSI" -> atrExpanding && rsiImpulse;
      case "Flat only" -> !atrExpanding || !rsiImpulse;
      default -> true;
    };
  }

  private static double highAtHighestBar(DataSeries s, int i, int len) {
    int start = Math.max(0, i - len + 1);
    int best = start;
    double max = s.getHigh(start);
    for (int j = start + 1; j <= i; j++) {
      double v = s.getHigh(j);
      if (v >= max) {
        max = v;
        best = j;
      }
    }
    return s.getHigh(best);
  }

  private static double lowAtLowestBar(DataSeries s, int i, int len) {
    int start = Math.max(0, i - len + 1);
    int best = start;
    double min = s.getLow(start);
    for (int j = start + 1; j <= i; j++) {
      double v = s.getLow(j);
      if (v <= min) {
        min = v;
        best = j;
      }
    }
    return s.getLow(best);
  }

  private static double smaHigh(DataSeries s, int i, int len) {
    if (i + 1 < len) return Double.NaN;
    double sum = 0.0;
    for (int j = i - len + 1; j <= i; j++) sum += s.getHigh(j);
    return sum / len;
  }

  private static double smaLow(DataSeries s, int i, int len) {
    if (i + 1 < len) return Double.NaN;
    double sum = 0.0;
    for (int j = i - len + 1; j <= i; j++) sum += s.getLow(j);
    return sum / len;
  }

  private static double[] tsi(double[] close, int shortLen, int longLen) {
    double[] momentum = new double[close.length];
    double[] absMomentum = new double[close.length];
    momentum[0] = Double.NaN;
    absMomentum[0] = Double.NaN;
    for (int i = 1; i < close.length; i++) {
      momentum[i] = close[i] - close[i - 1];
      absMomentum[i] = Math.abs(momentum[i]);
    }
    double[] smoothMomentum = emaWithNa(emaWithNa(momentum, shortLen, 2.0 / (shortLen + 1.0)), longLen, 2.0 / (longLen + 1.0));
    double[] smoothAbs = emaWithNa(emaWithNa(absMomentum, shortLen, 2.0 / (shortLen + 1.0)), longLen, 2.0 / (longLen + 1.0));
    double[] out = new double[close.length];
    for (int i = 0; i < close.length; i++) {
      out[i] = smoothAbs[i] == 0.0 || Double.isNaN(smoothAbs[i]) ? Double.NaN : 100.0 * smoothMomentum[i] / smoothAbs[i];
    }
    return out;
  }

  private static double highest(double[] v, int i, int len) {
    double max = Double.NEGATIVE_INFINITY;
    for (int j = Math.max(0, i - len + 1); j <= i; j++) max = Math.max(max, v[j]);
    return max;
  }

  private static double lowest(double[] v, int i, int len) {
    double min = Double.POSITIVE_INFINITY;
    for (int j = Math.max(0, i - len + 1); j <= i; j++) min = Math.min(min, v[j]);
    return min;
  }

  private static double nz(double v) {
    return Double.isNaN(v) || Double.isInfinite(v) ? 0.0 : v;
  }
}
