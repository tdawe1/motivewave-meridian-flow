package mwext.meridian;

import com.motivewave.platform.sdk.common.DrawContext;
import com.motivewave.platform.sdk.draw.Figure;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.imageio.ImageIO;

final class SignalImageFigure extends Figure {
  private static final int MAX_IMAGE_CACHE_ENTRIES = 16;
  private static final Map<String, BufferedImage> IMAGE_CACHE = new LinkedHashMap<>(MAX_IMAGE_CACHE_ENTRIES, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, BufferedImage> eldest) {
      return size() > MAX_IMAGE_CACHE_ENTRIES;
    }
  };

  private final long time;
  private final double value;
  private final File imageFile;
  private final int size;
  private final int offset;
  private final String fallbackText;
  private final Color fallbackColor;

  SignalImageFigure(long time, double value, File imageFile, int size, int offset, String fallbackText, Color fallbackColor) {
    this.time = time;
    this.value = value;
    this.imageFile = imageFile;
    this.size = Math.max(16, Math.min(size, 180));
    this.offset = Math.max(0, Math.min(offset, 80));
    this.fallbackText = fallbackText == null ? "" : fallbackText;
    this.fallbackColor = fallbackColor == null ? Color.WHITE : fallbackColor;
    setPopupMessage(this.fallbackText);
  }

  @Override
  public void layout(DrawContext ctx) {
    Point2D p = ctx == null ? null : ctx.translate(time, value);
    if (p == null) {
      setBounds(new Rectangle2D.Double(0, 0, size, size));
      return;
    }
    double x = p.getX() - size / 2.0;
    double y = p.getY() - size - offset;
    setBounds(new Rectangle2D.Double(x, y, size, size));
  }

  @Override
  public void draw(Graphics2D gc, DrawContext ctx) {
    if (gc == null) return;
    layout(ctx);
    Rectangle2D b = getBounds();
    BufferedImage image = loadImage(imageFile);

    Graphics2D g = (Graphics2D)gc.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int x = (int)Math.round(b.getX());
      int y = (int)Math.round(b.getY());
      int w = (int)Math.round(b.getWidth());
      int h = (int)Math.round(b.getHeight());
      if (image != null) {
        g.drawImage(image, x, y, w, h, null);
        g.setColor(new Color(255, 255, 255, 185));
        g.drawRoundRect(x, y, w, h, 8, 8);
      }
      else {
        g.setColor(new Color(8, 12, 18, 220));
        g.fillRoundRect(x, y, w, h, 10, 10);
        g.setColor(fallbackColor);
        g.drawRoundRect(x, y, w, h, 10, 10);
        g.drawString(fallbackText, x + 6, y + Math.max(14, h / 2));
      }
    }
    finally {
      g.dispose();
    }
  }

  private static BufferedImage loadImage(File file) {
    if (file == null) return null;
    String key = file.getAbsolutePath();
    synchronized (IMAGE_CACHE) {
      if (IMAGE_CACHE.containsKey(key)) return IMAGE_CACHE.get(key);
      BufferedImage image = null;
      if (file.isFile()) {
        try {
          image = ImageIO.read(file);
        }
        catch (Exception ignored) {
          image = null;
        }
      }
      IMAGE_CACHE.put(key, image);
      return image;
    }
  }
}
