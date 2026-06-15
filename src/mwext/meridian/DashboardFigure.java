package mwext.meridian;

import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.draw.Figure;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

final class DashboardFigure extends Figure {
  static final Color TEXT = new Color(220, 228, 235);
  static final Color ACCENT = new Color(70, 188, 255);
  static final Color GOOD = new Color(0, 230, 118);
  static final Color WARN = new Color(255, 193, 7);
  static final Color BAD = new Color(255, 82, 82);

  private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);
  private static final Font FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);
  private static final Stroke BORDER_STROKE = new BasicStroke(1.2f);
  private static final Color BACKGROUND = new Color(6, 10, 14, 228);
  private static final Color HEADER = new Color(20, 44, 58, 230);
  private static final Color BORDER = new Color(90, 170, 220, 130);
  private static final Color MUTED = new Color(155, 166, 176);
  private static final Color APPLY_BG = new Color(255, 152, 0, 220);
  private static final int PAD_X = 10;
  private static final int PAD_Y = 8;
  private static final int ROW_H = 17;
  private static final int FULL_MIN_W = 560;
  private static final int FULL_MAX_W = 820;
  private static final int COMPACT_MIN_W = 300;
  private static final int COMPACT_MAX_W = 430;
  private static final int APPLY_BTN_H = 22;

  private final DashboardRow[] rows;
  private final int count;
  private final boolean compact;
  private final String posPreset;
  private final int xOffset;
  private final int yOffset;
  private final double scale;
  private final boolean optStale;

  private Rectangle2D applyBounds;

  DashboardFigure(DashboardRow[] rows, int count, boolean compact,
                  String posPreset, int xOffset, int yOffset, double scale,
                  boolean optStale) {
    this.count = Math.max(0, Math.min(count, rows == null ? 0 : rows.length));
    this.rows = new DashboardRow[this.count];
    this.compact = compact;
    this.posPreset = posPreset == null ? "Top Left" : posPreset;
    this.xOffset = Math.max(0, Math.min(xOffset, 200));
    this.yOffset = Math.max(0, Math.min(yOffset, 200));
    this.scale = Math.max(0.5, Math.min(scale, 2.0));
    this.optStale = optStale;
    if (this.count > 0) System.arraycopy(rows, 0, this.rows, 0, this.count);
  }

  Rectangle2D applyBounds() { return applyBounds; }

  @Override
  public void layout(DrawContext ctx) {
    Rectangle chart = ctx == null ? null : ctx.getBounds();
    if (chart == null) {
      setBounds(new Rectangle2D.Double(0, 0, COMPACT_MIN_W, PAD_Y * 2 + ROW_H));
      return;
    }
    int chartW = chart.width;
    int chartH = chart.height;

    int minW = (int)Math.round((compact ? COMPACT_MIN_W : FULL_MIN_W) * scale);
    int maxW = (int)Math.round((compact ? COMPACT_MAX_W : FULL_MAX_W) * scale);
    int width = Math.max(minW, Math.min(maxW, chartW - 24));
    width = Math.min(width, chartW);

    int height = PAD_Y * 2 + Math.max(1, count) * ROW_H;
    if (optStale) height += APPLY_BTN_H + 4;
    height = Math.min(height, chartH);

    int ox = Math.max(0, Math.min(xOffset, chartW - width));
    int oy = Math.max(0, Math.min(yOffset, chartH - height));

    int x, y;
    switch (posPreset) {
      case "Top Right" -> { x = chartW - width - ox; y = oy; }
      case "Bottom Left" -> { x = ox; y = chartH - height - oy; }
      case "Bottom Right" -> { x = chartW - width - ox; y = chartH - height - oy; }
      default -> { x = ox; y = oy; }
    }

    x = Math.max(0, Math.min(x, chartW - width));
    y = Math.max(0, Math.min(y, chartH - height));
    x += chart.x;
    y += chart.y;

    setBounds(new Rectangle2D.Double(x, y, width, height));
  }

  @Override
  public void draw(Graphics2D gc, DrawContext ctx) {
    if (gc == null || count == 0) return;
    layout(ctx);
    Rectangle2D b = getBounds();
    int x = (int)Math.round(b.getX());
    int y = (int)Math.round(b.getY());
    int w = (int)Math.round(b.getWidth());
    int h = (int)Math.round(b.getHeight());

    Graphics2D g = (Graphics2D) gc.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(BACKGROUND);
      g.fillRoundRect(x, y, w, h, 12, 12);
      g.setColor(BORDER);
      g.setStroke(BORDER_STROKE);
      g.drawRoundRect(x, y, w, h, 12, 12);

      int rowY = y + PAD_Y;
      for (int i = 0; i < count; i++) {
        DashboardRow row = rows[i];
        if (row == null) continue;
        drawRow(g, row, x + PAD_X, rowY, w - PAD_X * 2, compact);
        rowY += ROW_H;
      }

      if (optStale) {
        int btnX = x + PAD_X;
        int btnY = y + h - APPLY_BTN_H - 6;
        int btnW = w - PAD_X * 2;
        applyBounds = new Rectangle2D.Double(btnX, btnY, btnW, APPLY_BTN_H);

        g.setColor(APPLY_BG);
        g.fillRoundRect(btnX, btnY, btnW, APPLY_BTN_H, 8, 8);
        g.setColor(Color.WHITE);
        g.setFont(TITLE_FONT);
        FontMetrics fm = g.getFontMetrics();
        String label = "\u25B6 APPLY OPTIMIZER";
        int labelW = fm.stringWidth(label);
        g.drawString(label, btnX + (btnW - labelW) / 2, btnY + 15);
      } else {
        applyBounds = null;
      }
    } finally {
      g.dispose();
    }
  }

  private static void drawRow(Graphics2D gc, DashboardRow row, int x, int y, int width, boolean compact) {
    if (row.header) {
      gc.setColor(HEADER);
      gc.fillRoundRect(x - 5, y - 1, width + 10, ROW_H - 1, 8, 8);
      gc.setFont(TITLE_FONT);
    }
    else {
      gc.setFont(FONT);
    }

    FontMetrics fm = gc.getFontMetrics();
    int baseline = y + 12;
    int labelW = compact ? (row.header ? 54 : 48) : (row.header ? 150 : 128);
    int extraCap = compact ? 92 : 250;
    int extraW = row.extra == null || row.extra.isEmpty() ? 0 : Math.min(extraCap, fm.stringWidth(row.extra));
    int valueX = x + labelW;
    int extraX = x + width - extraW;
    int valueW = Math.max(20, extraX - valueX - 8);

    gc.setColor(row.header ? TEXT : MUTED);
    gc.drawString(clip(row.label, fm, labelW - 8), x, baseline);
    gc.setColor(row.valueColor == null ? TEXT : row.valueColor);
    gc.drawString(clip(row.value, fm, valueW), valueX, baseline);
    if (extraW > 0) {
      gc.setColor(row.header ? row.valueColor : MUTED);
      gc.drawString(clip(row.extra, fm, extraW), extraX, baseline);
    }
  }

  private static String clip(String text, FontMetrics fm, int maxWidth) {
    if (text == null) return "";
    if (maxWidth <= 0 || fm.stringWidth(text) <= maxWidth) return text;
    if (maxWidth <= fm.stringWidth("\u2026")) return "";
    int lo = 0;
    int hi = text.length();
    while (lo < hi) {
      int mid = (lo + hi + 1) >>> 1;
      if (fm.stringWidth(text.substring(0, mid) + "\u2026") <= maxWidth) lo = mid;
      else hi = mid - 1;
    }
    return lo <= 0 ? "" : text.substring(0, lo) + "\u2026";
  }
}
