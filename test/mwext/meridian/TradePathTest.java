package mwext.meridian;

public final class TradePathTest {
  public static void main(String[] args) {
    assertFalse(MeridianBacktest.targetHitBeforeStop(1, 100, 106, 94, 105, 95, 105),
      "bullish long bar visits low before target");
    assertTrue(MeridianBacktest.targetHitBeforeStop(1, 100, 106, 94, 96, 95, 105),
      "bearish long bar visits target before low");

    assertTrue(MeridianBacktest.targetHitBeforeStop(-1, 100, 106, 94, 104, 105, 95),
      "bullish short bar visits target before high");
    assertFalse(MeridianBacktest.targetHitBeforeStop(-1, 100, 106, 94, 96, 105, 95),
      "bearish short bar visits high before target");

    assertTrue(MeridianBacktest.targetHitBeforeStop(1, 100, 106, 99, 104, 95, 105),
      "target-only long bar");
    assertFalse(MeridianBacktest.targetHitBeforeStop(1, 100, 101, 94, 99, 95, 105),
      "stop-only long bar");
    assertFalse(MeridianBacktest.targetHitBeforeStop(1, 100, 110, 90, 100, 90, 110),
      "doji long bar visits low before target");
  }

  private static void assertTrue(boolean value, String label) {
    if (!value) throw new AssertionError(label + ": expected true");
  }

  private static void assertFalse(boolean value, String label) {
    if (value) throw new AssertionError(label + ": expected false");
  }
}
