package mwext.meridian;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

public final class SettingsViewCopyTest {
  public static void main(String[] args) throws Exception {
    SettingsView original = populated();
    SettingsView copy = original.copy();

    for (Field field : SettingsView.class.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())) continue;
      Object expected = field.get(original);
      Object actual = field.get(copy);
      if (!Objects.equals(expected, actual)) {
        throw new AssertionError(field.getName() + " not copied: expected " + expected + ", got " + actual);
      }
    }

    SettingsView same = original.copy();
    assertEquals(original.tuningKey(), same.tuningKey(), "copied tuning key");

    SettingsView changedTuning = original.copy();
    changedTuning.swingLen++;
    assertNotEquals(original.tuningKey(), changedTuning.tuningKey(), "strategy field affects tuning key");

    SettingsView changedVisual = original.copy();
    changedVisual.bullColor = Color.MAGENTA;
    assertEquals(original.tuningKey(), changedVisual.tuningKey(), "visual field ignored by tuning key");
  }

  private static SettingsView populated() throws Exception {
    SettingsView cfg = new SettingsView();
    int i = 1;
    for (Field field : SettingsView.class.getDeclaredFields()) {
      if (Modifier.isStatic(field.getModifiers())) continue;
      Class<?> type = field.getType();
      if (type == int.class) field.setInt(cfg, 10 + i);
      else if (type == double.class) field.setDouble(cfg, 1.25 + i);
      else if (type == boolean.class) field.setBoolean(cfg, i % 2 == 0);
      else if (type == String.class) field.set(cfg, field.getName() + "-value");
      else if ("com.motivewave.platform.sdk.common.BarSize".equals(type.getName())) field.set(cfg, null);
      else if (type == File.class) field.set(cfg, new File("/tmp/" + field.getName()));
      else if (type == Color.class) field.set(cfg, new Color((i * 40) & 0xff, (i * 70) & 0xff, (i * 100) & 0xff));
      else throw new AssertionError("Unhandled SettingsView field type: " + field.getName() + " " + type);
      i++;
    }
    return cfg;
  }

  private static void assertEquals(String expected, String actual, String label) {
    if (!Objects.equals(expected, actual)) {
      throw new AssertionError(label + ": expected " + expected + ", got " + actual);
    }
  }

  private static void assertNotEquals(String expected, String actual, String label) {
    if (Objects.equals(expected, actual)) {
      throw new AssertionError(label + ": expected values to differ");
    }
  }
}
