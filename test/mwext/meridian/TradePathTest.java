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

    // Edge cases
    assertFalse(MeridianBacktest.targetHitBeforeStop(0, 100, 106, 94, 105, 95, 105),
      "dir=0 is always false");
    assertFalse(MeridianBacktest.targetHitBeforeStop(1, 100, 106, 94, 105, Double.NaN, 105),
      "NaN stop is always false");
    assertFalse(MeridianBacktest.targetHitBeforeStop(1, 100, 106, 94, 105, 95, Double.NaN),
      "NaN target is always false");
    assertFalse(MeridianBacktest.targetHitBeforeStop(1, 100, 106, 94, 105, 95, Double.POSITIVE_INFINITY),
      "infinite target is always false");

    // Short direction edge cases
    assertTrue(MeridianBacktest.targetHitBeforeStop(-1, 100, 101, 94, 96, 105, 95),
      "short target-only bar");
    assertFalse(MeridianBacktest.targetHitBeforeStop(-1, 100, 106, 99, 101, 105, 95),
      "short stop-only bar");

    // Target equals open (target hit at open)
    assertTrue(MeridianBacktest.targetHitBeforeStop(1, 105, 110, 99, 106, 95, 105),
      "long: target equals open");
    // Stop equals open (stop hit at open)
    assertFalse(MeridianBacktest.targetHitBeforeStop(1, 95, 110, 94, 106, 95, 105),
      "long: stop equals open");

    // Neither target nor stop hit
    assertFalse(MeridianBacktest.targetHitBeforeStop(1, 100, 102, 98, 101, 95, 105),
      "neither hit on bar");

    boolean[] longs = new boolean[4];
    boolean[] shorts = new boolean[4];
    MeridianIndicators.storeNonConflictingSignals(longs, shorts, 0, true, false);
    MeridianIndicators.storeNonConflictingSignals(longs, shorts, 1, false, true);
    MeridianIndicators.storeNonConflictingSignals(longs, shorts, 2, true, true);
    MeridianIndicators.storeNonConflictingSignals(longs, shorts, 3, false, false);
    assertTrue(longs[0], "long-only signal kept");
    assertFalse(shorts[0], "long-only short side clear");
    assertFalse(longs[1], "short-only long side clear");
    assertTrue(shorts[1], "short-only signal kept");
    assertFalse(longs[2], "conflicting long signal suppressed");
    assertFalse(shorts[2], "conflicting short signal suppressed");
    assertFalse(longs[3], "empty long remains clear");
    assertFalse(shorts[3], "empty short remains clear");
  }

  private static void assertTrue(boolean value, String label) {
    if (!value) throw new AssertionError(label + ": expected true");
  }

  private static void assertFalse(boolean value, String label) {
    if (value) throw new AssertionError(label + ": expected false");
  }
}
