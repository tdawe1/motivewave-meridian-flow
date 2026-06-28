package mwext.meridian;

public final class DashboardStateTest {
  public static void main(String[] args) {
    Projection nextLong = new Projection();
    nextLong.longValid = true;
    nextLong.longLabel = "NEXT LONG > swing";
    assertEquals("NEXT LONG", state(false, false, nextLong, 0, 101.0, 99.0, 105.0, 95.0, false, false, true, false, true, true, true, true), "ready projection");

    Projection waitLong = new Projection();
    waitLong.longValid = true;
    waitLong.longLabel = "WAIT LONG > swing";
    assertEquals("WAIT LONG", state(false, false, waitLong, 0, 101.0, 99.0, 105.0, 95.0, false, false, false, false, true, true, true, true), "wait projection");

    assertEquals("LIVE LONG", state(false, false, waitLong, -1, 106.0, 99.0, 105.0, 95.0, false, false, true, false, true, true, true, true), "live bull break");
    assertEquals("SHORT BREAK", state(false, false, null, 1, 101.0, 94.0, 105.0, 95.0, false, false, true, false, true, true, true, true), "live bear break without filter alignment");
    assertEquals("LONG BIAS", state(false, false, null, 1, 101.0, 99.0, 105.0, 95.0, false, false, true, false, true, true, true, true), "structure and filter bias");
    assertEquals("FORGE SHORT", state(false, false, null, 0, 101.0, 99.0, 105.0, 95.0, false, false, false, true, true, true, false, true), "forge-only bias");
    assertEquals("NEUTRAL", state(false, false, null, 0, 101.0, 99.0, 105.0, 95.0, true, true, false, false, true, true, true, true), "neutral");
  }

  private static String state(boolean longSignal, boolean shortSignal, Projection projection, int structTrend,
                              double breakHighSrc, double breakLowSrc, double lastSwingHigh, double lastSwingLow,
                              boolean swingHighBroken, boolean swingLowBroken,
                              boolean forgeLongNow, boolean forgeShortNow, boolean htfLongNow, boolean htfShortNow,
                              boolean usesStructure, boolean usesForge) {
    return DashboardSupport.stateLabel(longSignal, shortSignal, projection, structTrend,
      breakHighSrc, breakLowSrc, lastSwingHigh, lastSwingLow, swingHighBroken, swingLowBroken,
      forgeLongNow, forgeShortNow, htfLongNow, htfShortNow, usesStructure, usesForge);
  }

  private static void assertEquals(String expected, String actual, String label) {
    if (!expected.equals(actual)) {
      throw new AssertionError(label + ": expected " + expected + " but got " + actual);
    }
  }
}
