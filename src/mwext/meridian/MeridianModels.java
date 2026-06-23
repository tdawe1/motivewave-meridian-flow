package mwext.meridian;

import com.motivewave.platform.sdk.common.Enums$Position;

import java.awt.BasicStroke;
import java.awt.Color;

final class OBZone {
  double top;
  double bot;
  int dir;
  int left;
  int right;
  int created;
  boolean mitigated;
  boolean armed;
}

final class TradeLines {
  int dir;
  int entryIndex;
  int endIndex;
  double entry;
  double sl;
  double tp1;
  double tp2;
  double tp3;
  boolean tp1Hit;
  boolean tp2Hit;
  boolean tp3Hit;
  boolean beActive;
}

final class BacktestStats {
  int trades;
  int wins;
  int losses;
  int breakEvens;
  int stops;
  int tp1Hits;
  int tp2Hits;
  int tp3Hits;
  int activeDir;
  double activeEntry = Double.NaN;
  double activeUnrealized = Double.NaN;
  double netPoints;
  double peakPoints;
  double maxDrawdownPoints;
  double grossWinPoints;
  double grossLossPoints;
}

final class Projection {
  int index;
  int endIndex;
  boolean longValid;
  boolean shortValid;
  double longEntry;
  double longSL;
  double longTP1;
  double longTP2;
  double longTP3;
  double shortEntry;
  double shortSL;
  double shortTP1;
  double shortTP2;
  double shortTP3;
  String longLabel;
  String shortLabel;
}

final class DashboardRow {
  final String label;
  final String value;
  final String extra;
  final Color valueColor;
  final boolean header;

  DashboardRow(String label, String value, String extra, Color valueColor, boolean header) {
    this.label = label;
    this.value = value;
    this.extra = extra;
    this.valueColor = valueColor;
    this.header = header;
  }
}

final class SignalArrays {
  final boolean[] longs;
  final boolean[] shorts;

  SignalArrays(boolean[] longs, boolean[] shorts) {
    this.longs = longs;
    this.shorts = shorts;
  }
}

final class TradingBotResult {
  final boolean[] longSignal;
  final boolean[] shortSignal;
  final boolean[] halfTrendLong;
  final boolean[] halfTrendShort;
  final boolean[] trendLong;
  final boolean[] trendShort;
  final boolean[] tsiCurlLong;
  final boolean[] tsiCurlShort;
  final boolean[] ademaLong;
  final boolean[] ademaShort;
  final int[] halfTrendDir;
  final int[] trendDir;
  final double[] halfTrendLine;
  final double[] trendLine;
  final double[] adaptiveEma;

  TradingBotResult(int n) {
    longSignal = new boolean[n];
    shortSignal = new boolean[n];
    halfTrendLong = new boolean[n];
    halfTrendShort = new boolean[n];
    trendLong = new boolean[n];
    trendShort = new boolean[n];
    tsiCurlLong = new boolean[n];
    tsiCurlShort = new boolean[n];
    ademaLong = new boolean[n];
    ademaShort = new boolean[n];
    halfTrendDir = new int[n];
    trendDir = new int[n];
    halfTrendLine = new double[n];
    trendLine = new double[n];
    adaptiveEma = new double[n];
    for (int i = 0; i < n; i++) {
      halfTrendLine[i] = Double.NaN;
      trendLine[i] = Double.NaN;
      adaptiveEma[i] = Double.NaN;
    }
  }
}

final class ForgeState {
  final boolean longOk;
  final boolean shortOk;

  ForgeState(boolean longOk, boolean shortOk) {
    this.longOk = longOk;
    this.shortOk = shortOk;
  }
}

final class Macd {
  final double[] line;
  final double[] signal;

  Macd(double[] line, double[] signal) {
    this.line = line;
    this.signal = signal;
  }
}

final class Stoch {
  final double[] k;
  final double[] d;

  Stoch(double[] k, double[] d) {
    this.k = k;
    this.d = d;
  }
}
final class Bands {
  final double[] mid;
  final double[] upper;
  final double[] lower;

  Bands(double[] mid, double[] upper, double[] lower) {
    this.mid = mid;
    this.upper = upper;
    this.lower = lower;
  }
}


final class Adx {
  final double[] plus;
  final double[] minus;
  final double[] adx;

  Adx(double[] plus, double[] minus, double[] adx) {
    this.plus = plus;
    this.minus = minus;
    this.adx = adx;
  }
}

final class Sar {
  final double[] value;

  Sar(double[] value) {
    this.value = value;
  }
}

final class Tilson {
  final double[] value;
  final double[] price;

  Tilson(double[] value, double[] price) {
    this.value = value;
    this.price = price;
  }
}

final class Smi {
  final double[] value;
  final double[] signal;
  final boolean[] crossAbove;
  final boolean[] crossBelow;

  Smi(double[] value, double[] signal, boolean[] crossAbove, boolean[] crossBelow) {
    this.value = value;
    this.signal = signal;
    this.crossAbove = crossAbove;
    this.crossBelow = crossBelow;
  }
}

final class Super {
  final double[] line;
  final int[] dir;

  Super(double[] line, int[] dir) {
    this.line = line;
    this.dir = dir;
  }
}

final class HtfBias {
  final boolean[] bull;
  final boolean[] bear;

  HtfBias(boolean[] bull, boolean[] bear) {
    this.bull = bull;
    this.bear = bear;
  }
}

final class FigureEvent {
  int type;
  int x1;
  int x2;
  double y1;
  double y2;
  String text;
  Color color;
  Enums$Position pos;
  BasicStroke stroke;

  static FigureEvent line(int x1, double y1, int x2, double y2, Color c, BasicStroke stroke) {
    FigureEvent e = new FigureEvent();
    e.type = 1;
    e.x1 = x1;
    e.y1 = y1;
    e.x2 = x2;
    e.y2 = y2;
    e.color = c;
    e.stroke = stroke;
    return e;
  }

  static FigureEvent label(int x, double y, String text, Color c, Enums$Position pos) {
    FigureEvent e = new FigureEvent();
    e.type = 2;
    e.x1 = x;
    e.y1 = y;
    e.text = text;
    e.color = c;
    e.pos = pos;
    return e;
  }
}
