package mwext.meridian;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;

import java.util.Arrays;

final class MeridianIndicators {
  private MeridianIndicators() {}

  static double[] closeArray(DataSeries s) {
    double[] out = new double[s.size()];
    for (int i = 0; i < out.length; i++) out[i] = s.getClose(i);
    return out;
  }

  static double[] sourceArray(DataSeries s, String source) {
    double[] out = new double[s.size()];
    for (int i = 0; i < out.length; i++) {
      out[i] = switch (source == null ? MeridianOptions.CLOSE : source) {
        case MeridianOptions.OPEN -> s.getOpen(i);
        case MeridianOptions.HIGH -> s.getHigh(i);
        case MeridianOptions.LOW -> s.getLow(i);
        case MeridianOptions.HL2 -> (s.getHigh(i) + s.getLow(i)) / 2.0;
        case MeridianOptions.HLC3 -> (s.getHigh(i) + s.getLow(i) + s.getClose(i)) / 3.0;
        default -> s.getClose(i);
      };
    }
    return out;
  }

  static double[] maSeries(String method, double[] v, int len) {
    if (MeridianOptions.EMA.equals(method)) return emaWithNa(v, len, 2.0 / (len + 1.0));
    if (MeridianOptions.MEMA.equals(method)) return emaWithNa(v, len, 1.0 / len);
    return sma(v, len);
  }

  static double[] emaWithNa(double[] v, int len, double k) {
    double[] out = fillNa(v.length);
    double sum = 0.0;
    int valid = 0;
    boolean ready = false;
    for (int i = 0; i < v.length; i++) {
      double val = v[i];
      if (Double.isNaN(val) || Double.isInfinite(val)) {
        if (ready) out[i] = out[i - 1];
        continue;
      }
      if (!ready) {
        sum += val;
        valid++;
        if (valid == len) {
          out[i] = sum / len;
          ready = true;
        }
      }
      else {
        out[i] = val * k + out[i - 1] * (1.0 - k);
      }
    }
    return out;
  }

  static Tilson tilson(DataSeries s, String input, String method, int period) {
    int n = s.size();
    double[] price = sourceArray(s, input);
    double[] av = maSeries(method, price, period);
    double[] ie2 = fillNa(n);
    double sumX = period * (period - 1) / 2.0;
    double sumXX = (period - 1) * period * (2.0 * period - 1.0) / 6.0;
    double denom = period * sumXX - sumX * sumX;
    if (denom == 0.0) return new Tilson(ie2, price);
    for (int i = period - 1; i < n; i++) {
      if (Double.isNaN(av[i])) continue;
      double sumY = 0.0;
      double sumXY = 0.0;
      boolean ok = true;
      int start = i - period + 1;
      for (int x = 0; x < period; x++) {
        double y = price[start + x];
        if (Double.isNaN(y) || Double.isInfinite(y)) { ok = false; break; }
        sumY += y;
        sumXY += x * y;
      }
      if (!ok) continue;
      double slope = (period * sumXY - sumX * sumY) / denom;
      double intercept = (sumY - slope * sumX) / period;
      double regressionNow = intercept + slope * (period - 1);
      ie2[i] = (regressionNow + av[i] + slope) / 2.0;
    }
    return new Tilson(ie2, price);
  }

  static Smi smi(DataSeries s, String input, String method, int longPeriod, int shortPeriod, int signalPeriod) {
    int n = s.size();
    double[] price = sourceArray(s, input);
    double[] change = fillNa(n);
    double[] absChange = fillNa(n);
    for (int i = 1; i < n; i++) {
      change[i] = price[i] - price[i - 1];
      absChange[i] = Math.abs(change[i]);
    }
    double[] tempChange = maSeries(method, maSeries(method, change, shortPeriod), longPeriod);
    double[] tempAbsChange = maSeries(method, maSeries(method, absChange, shortPeriod), longPeriod);
    double[] value = fillNa(n);
    for (int i = 0; i < n; i++) {
      if (!Double.isNaN(tempChange[i]) && !Double.isNaN(tempAbsChange[i]) && tempAbsChange[i] != 0.0) {
        value[i] = tempChange[i] / tempAbsChange[i];
      }
    }
    double[] signal = maSeries(method, value, signalPeriod);
    boolean[] crossAbove = new boolean[n];
    boolean[] crossBelow = new boolean[n];
    for (int i = 1; i < n; i++) {
      if (Double.isNaN(value[i]) || Double.isNaN(value[i - 1]) || Double.isNaN(signal[i]) || Double.isNaN(signal[i - 1])) continue;
      crossAbove[i] = value[i - 1] <= signal[i - 1] && value[i] > signal[i];
      crossBelow[i] = value[i - 1] >= signal[i - 1] && value[i] < signal[i];
    }
    return new Smi(value, signal, crossAbove, crossBelow);
  }

  static double[] sma(DataSeries s, int len) {
    return sma(closeArray(s), len);
  }

  static double[] sma(double[] v, int len) {
    double[] out = fillNa(v.length);
    double sum = 0;
    int valid = 0;
    for (int i = 0; i < v.length; i++) {
      if (!Double.isNaN(v[i])) {
        sum += v[i];
        valid++;
      }
      if (i >= len && !Double.isNaN(v[i - len])) {
        sum -= v[i - len];
        valid--;
      }
      if (i >= len - 1 && valid == len) out[i] = sum / len;
    }
    return out;
  }
  static Bands bollinger(double[] v, int len, double mult) {
    double[] mid = sma(v, len);
    double[] upper = fillNa(v.length);
    double[] lower = fillNa(v.length);
    for (int i = len - 1; i < v.length; i++) {
      if (Double.isNaN(mid[i])) continue;
      double sumSq = 0.0;
      int valid = 0;
      for (int j = i - len + 1; j <= i; j++) {
        double value = v[j];
        if (Double.isNaN(value) || Double.isInfinite(value)) break;
        double delta = value - mid[i];
        sumSq += delta * delta;
        valid++;
      }
      if (valid != len) continue;
      double width = Math.sqrt(sumSq / len) * mult;
      upper[i] = mid[i] + width;
      lower[i] = mid[i] - width;
    }
    return new Bands(mid, upper, lower);
  }


  static double[] ema(DataSeries s, int len) {
    return ema(closeArray(s), len);
  }

  static double[] ema(double[] v, int len) {
    double[] out = fillNa(v.length);
    double k = 2.0 / (len + 1.0);
    double sum = 0;
    for (int i = 0; i < v.length; i++) {
      double val = nz(v[i]);
      if (i < len) {
        sum += val;
        if (i == len - 1) out[i] = sum / len;
      }
      else {
        out[i] = val * k + out[i - 1] * (1.0 - k);
      }
    }
    return out;
  }

  static double[] rsi(DataSeries s, int len) {
    int n = s.size();
    double[] out = fillNa(n);
    double gain = 0, loss = 0;
    for (int i = 1; i < n; i++) {
      double ch = s.getClose(i) - s.getClose(i - 1);
      double g = Math.max(ch, 0);
      double l = Math.max(-ch, 0);
      if (i <= len) {
        gain += g; loss += l;
        if (i == len) {
          gain /= len; loss /= len;
          out[i] = loss == 0 ? (gain == 0 ? 50 : 100) : 100 - 100 / (1 + gain / loss);
        }
      }
      else {
        gain = (gain * (len - 1) + g) / len;
        loss = (loss * (len - 1) + l) / len;
        out[i] = loss == 0 ? (gain == 0 ? 50 : 100) : 100 - 100 / (1 + gain / loss);
      }
    }
    return out;
  }

  static Macd macd(DataSeries s, int fast, int slow, int sig) {
    double[] ef = ema(s, fast);
    double[] es = ema(s, slow);
    double[] line = fillNa(s.size());
    for (int i = 0; i < line.length; i++) {
      if (!Double.isNaN(ef[i]) && !Double.isNaN(es[i])) line[i] = ef[i] - es[i];
    }
    return new Macd(line, emaWithNa(line, sig, 2.0 / (sig + 1.0)));
  }

  static Stoch stoch(DataSeries s, int kLen, int dLen, int smooth) {
    int n = s.size();
    double[] raw = fillNa(n);
    for (int i = kLen - 1; i < n; i++) {
      double hh = -Double.MAX_VALUE, ll = Double.MAX_VALUE;
      for (int j = i - kLen + 1; j <= i; j++) {
        hh = Math.max(hh, s.getHigh(j));
        ll = Math.min(ll, s.getLow(j));
      }
      raw[i] = hh == ll ? 50 : 100 * (s.getClose(i) - ll) / (hh - ll);
    }
    double[] k = sma(raw, smooth);
    return new Stoch(k, sma(k, dLen));
  }

  static double[] ao(DataSeries s) {
    int n = s.size();
    double[] hl2 = new double[n];
    for (int i = 0; i < n; i++) hl2[i] = (s.getHigh(i) + s.getLow(i)) / 2.0;
    double[] fast = sma(hl2, 5);
    double[] slow = sma(hl2, 34);
    double[] out = fillNa(n);
    for (int i = 0; i < n; i++) if (!Double.isNaN(fast[i]) && !Double.isNaN(slow[i])) out[i] = fast[i] - slow[i];
    return out;
  }

  static double[] cci(DataSeries s, int len) {
    int n = s.size();
    double[] tp = new double[n];
    for (int i = 0; i < n; i++) tp[i] = (s.getHigh(i) + s.getLow(i) + s.getClose(i)) / 3.0;
    double[] ma = sma(tp, len);
    double[] out = fillNa(n);
    for (int i = len - 1; i < n; i++) {
      double dev = 0;
      for (int j = i - len + 1; j <= i; j++) dev += Math.abs(tp[j] - ma[i]);
      dev /= len;
      out[i] = dev == 0 ? 0 : (tp[i] - ma[i]) / (0.015 * dev);
    }
    return out;
  }

  static Adx adx(DataSeries s, int diLen, int adxLen) {
    int n = s.size();
    double[] tr = new double[n], plusDm = new double[n], minusDm = new double[n];
    for (int i = 1; i < n; i++) {
      double up = s.getHigh(i) - s.getHigh(i - 1);
      double down = s.getLow(i - 1) - s.getLow(i);
      plusDm[i] = up > down && up > 0 ? up : 0;
      minusDm[i] = down > up && down > 0 ? down : 0;
      tr[i] = Math.max(s.getHigh(i) - s.getLow(i),
        Math.max(Math.abs(s.getHigh(i) - s.getClose(i - 1)), Math.abs(s.getLow(i) - s.getClose(i - 1))));
    }
    double[] atr = rma(tr, diLen);
    double[] plus = rma(plusDm, diLen);
    double[] minus = rma(minusDm, diLen);
    double[] pdi = fillNa(n), mdi = fillNa(n), dx = fillNa(n);
    for (int i = 0; i < n; i++) {
      if (atr[i] == 0 || Double.isNaN(atr[i])) continue;
      pdi[i] = 100 * plus[i] / atr[i];
      mdi[i] = 100 * minus[i] / atr[i];
      double sum = pdi[i] + mdi[i];
      dx[i] = sum == 0 ? 0 : 100 * Math.abs(pdi[i] - mdi[i]) / sum;
    }
    return new Adx(pdi, mdi, rmaNa(dx, adxLen));
  }

  static Sar sar(DataSeries s, double start, double inc, double max) {
    int n = s.size();
    double[] out = fillNa(n);
    if (n == 0) return new Sar(out);
    boolean up = true;
    double af = start;
    double ep = s.getHigh(0);
    out[0] = s.getLow(0);
    for (int i = 1; i < n; i++) {
      double sar = out[i - 1] + af * (ep - out[i - 1]);
      if (up) {
        sar = Math.min(sar, s.getLow(i - 1));
        if (i > 1) sar = Math.min(sar, s.getLow(i - 2));
        if (s.getLow(i) < sar) {
          up = false; sar = ep; ep = s.getLow(i); af = start;
        }
        else if (s.getHigh(i) > ep) {
          ep = s.getHigh(i); af = Math.min(max, af + inc);
        }
      }
      else {
        sar = Math.max(sar, s.getHigh(i - 1));
        if (i > 1) sar = Math.max(sar, s.getHigh(i - 2));
        if (s.getHigh(i) > sar) {
          up = true; sar = ep; ep = s.getHigh(i); af = start;
        }
        else if (s.getLow(i) < ep) {
          ep = s.getLow(i); af = Math.min(max, af + inc);
        }
      }
      out[i] = sar;
    }
    return new Sar(out);
  }

  static Super superTrend(DataSeries s, int len, double factor) {
    int n = s.size();
    double[] atr = atr(s, len);
    double[] line = fillNa(n);
    int[] dir = new int[n];
    double prevUpper = Double.NaN, prevLower = Double.NaN;
    int trend = 1;
    for (int i = 0; i < n; i++) {
      if (Double.isNaN(atr[i])) continue;
      double hl2 = (s.getHigh(i) + s.getLow(i)) / 2.0;
      double upper = hl2 + factor * atr[i];
      double lower = hl2 - factor * atr[i];
      if (i > 0) {
        if (!Double.isNaN(prevUpper) && s.getClose(i - 1) <= prevUpper) upper = Math.min(upper, prevUpper);
        if (!Double.isNaN(prevLower) && s.getClose(i - 1) >= prevLower) lower = Math.max(lower, prevLower);
        if (trend == 1 && s.getClose(i) < lower) trend = -1;
        else if (trend == -1 && s.getClose(i) > upper) trend = 1;
      }
      line[i] = trend == 1 ? lower : upper;
      dir[i] = trend == 1 ? -1 : 1;
      prevUpper = upper; prevLower = lower;
    }
    return new Super(line, dir);
  }

  static ForgeState forgeState(SettingsView c, int i, double[] smaFast, double[] smaSlow, double[] emaFast,
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
      if (SmiModeOption.ZERO_BIAS.label.equals(c.smiMode)) {
        smiLong = smiLong && smi.value[i] > 0.0;
        smiShort = smiShort && smi.value[i] < 0.0;
      }
      else if (SmiModeOption.GUIDED_REVERSAL.label.equals(c.smiMode)) {
        smiLong = smiReady && smi.crossAbove[i] && smi.value[i] < c.smiBottomGuide;
        smiShort = smiReady && smi.crossBelow[i] && smi.value[i] > c.smiTopGuide;
      }
      longCond = merge(c.requireAll, longCond, smiLong);
      shortCond = merge(c.requireAll, shortCond, smiShort);
    }
    return any ? new ForgeState(longCond, shortCond) : new ForgeState(false, false);
  }

  static HtfBias htfBias(SettingsView cfg, DataContext ctx, DataSeries base, int n) {
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

  static double[] atr(DataSeries s, int len) {
    int n = s.size();
    double[] tr = new double[n];
    for (int i = 0; i < n; i++) {
      if (i == 0) tr[i] = s.getHigh(i) - s.getLow(i);
      else tr[i] = Math.max(s.getHigh(i) - s.getLow(i),
        Math.max(Math.abs(s.getHigh(i) - s.getClose(i - 1)), Math.abs(s.getLow(i) - s.getClose(i - 1))));
    }
    return rma(tr, len);
  }

  static double[] rma(double[] v, int len) {
    double[] out = fillNa(v.length);
    double sum = 0;
    for (int i = 0; i < v.length; i++) {
      double val = nz(v[i]);
      if (i < len) {
        sum += val;
        if (i == len - 1) out[i] = sum / len;
      }
      else {
        out[i] = (out[i - 1] * (len - 1) + val) / len;
      }
    }
    return out;
  }

  static double[] rmaNa(double[] v, int len) {
    double[] out = fillNa(v.length);
    double sum = 0.0;
    int valid = 0;
    boolean ready = false;
    for (int i = 0; i < v.length; i++) {
      double val = v[i];
      if (Double.isNaN(val) || Double.isInfinite(val)) {
        if (ready) out[i] = out[i - 1];
        continue;
      }
      if (!ready) {
        sum += val;
        valid++;
        if (valid == len) { out[i] = sum / len; ready = true; }
      }
      else {
        out[i] = (out[i - 1] * (len - 1) + val) / len;
      }
    }
    return out;
  }

  static double[] fillNa(int n) {
    double[] out = new double[n];
    for (int i = 0; i < n; i++) out[i] = Double.NaN;
    return out;
  }

  static double nz(double v) {
    return Double.isNaN(v) || Double.isInfinite(v) ? 0 : v;
  }

  private static boolean merge(boolean requireAll, boolean current, boolean next) {
    return requireAll ? current && next : current || next;
  }
}
