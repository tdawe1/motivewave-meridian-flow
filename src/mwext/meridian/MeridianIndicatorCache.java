package mwext.meridian;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;

import java.util.HashMap;
import java.util.Map;

// Shared implementation; each calculator or optimizer run owns its own snapshot-scoped instance.
final class MeridianIndicatorCache {
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
  private final Map<Long, double[]> atrTrend = new HashMap<>();
  private final Map<Long, Macd> macd = new HashMap<>();
  private final Map<Long, Stoch> stoch = new HashMap<>();
  private final Map<String, Bands> bands = new HashMap<>();
  private final Map<String, Sar> sar = new HashMap<>();
  private final Map<Long, Adx> adx = new HashMap<>();
  private final Map<String, Super> superTrend = new HashMap<>();
  private final Map<String, Tilson> tilson = new HashMap<>();
  private final Map<String, Smi> smi = new HashMap<>();
  private final Map<String, HtfBias> htf = new HashMap<>();

  MeridianIndicatorCache(DataContext ctx, DataSeries s) {
    this.ctx = ctx;
    this.s = s;
    this.n = s.size();
    this.closes = MeridianIndicators.closeArray(s);
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

  double[] atrTrend(int atrLen, int smooth) {
    long key = key(atrLen, smooth);
    double[] out = atrTrend.get(key);
    if (out == null) { out = MeridianIndicators.ema(atr(atrLen), smooth); atrTrend.put(key, out); }
    return out;
  }

  HtfBias htfBias(SettingsView cfg) {
    String key = cfg.useHtf + "|" + cfg.htfBarSize + "|" + cfg.htfEmaLen;
    HtfBias out = htf.get(key);
    if (out == null) { out = MeridianIndicators.htfBias(cfg, ctx, s, n); htf.put(key, out); }
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
