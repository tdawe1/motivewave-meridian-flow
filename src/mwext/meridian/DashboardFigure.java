package mwext.meridian;

import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.draw.Figure;

import java.awt.BasicStroke;
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
  private static final Color BACKGROUND = new Color(6, 10, 14, 228);
  private static final Color HEADER = new Color(20, 44, 58, 230);
  private static final Color BORDER = new Color(90, 170, 220, 130);
  private static final Color MUTED = new Color(155, 166, 176);
  private static final int PAD_X = 10;
  private static final int PAD_Y = 8;
  private static final int ROW_H = 17;
  private static final int MIN_W = 380;
  private static final int MAX_W = 520;

  private final DashboardRow[] rows;
  private final int count;

  DashboardFigure(DashboardRow[] rows, int count) {
    this.count = Math.max(0, Math.min(count, rows == null ? 0 : rows.length));
    this.rows = new DashboardRow[this.count];
    if (this.count > 0) System.arraycopy(rows, 0, this.rows, 0, this.count);
  }

  @Override
  public void layout(DrawContext ctx) {
    Rectangle chart = ctx == null ? null : ctx.getBounds();
    int height = PAD_Y * 2 + Math.max(1, count) * ROW_H;
    int width = MIN_W;
    if (chart != null) width = Math.max(MIN_W, Math.min(MAX_W, chart.width - 24));
    int x = chart == null ? 12 : chart.x + 12;
    int y = chart == null ? 12 : chart.y + 12;
    setBounds(new Rectangle2D.Double(x, y, width, height));
  }

  @Override
  public void draw(Graphics2D gc, DrawContext ctx) {
    if (gc == null || count == 0) return;
    if (getBounds() == null) layout(ctx);
    Rectangle2D b = getBounds();
    int x = (int)Math.round(b.getX());
    int y = (int)Math.round(b.getY());
    int w = (int)Math.round(b.getWidth());
    int h = (int)Math.round(b.getHeight());

    Object oldAa = gc.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
    gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    gc.setColor(BACKGROUND);
    gc.fillRoundRect(x, y, w, h, 12, 12);
    gc.setColor(BORDER);
    gc.setStroke(new BasicStroke(1.2f));
    gc.drawRoundRect(x, y, w, h, 12, 12);

    int rowY = y + PAD_Y;
    for (int i = 0; i < count; i++) {
      DashboardRow row = rows[i];
      if (row == null) continue;
      drawRow(gc, row, x + PAD_X, rowY, w - PAD_X * 2);
      rowY += ROW_H;
    }
    gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAa);
  }

  private static void drawRow(Graphics2D gc, DashboardRow row, int x, int y, int width) {
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
    int labelW = row.header ? 132 : 116;
    int extraW = row.extra == null || row.extra.isEmpty() ? 0 : Math.min(175, fm.stringWidth(row.extra));
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
    if (maxWidth <= fm.stringWidth("…")) return "";
    int end = text.length();
    while (end > 0 && fm.stringWidth(text.substring(0, end) + "…") > maxWidth) end--;
    return end <= 0 ? "" : text.substring(0, end) + "…";
  }
}
