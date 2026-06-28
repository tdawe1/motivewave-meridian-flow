package mwext.meridian;

import java.lang.reflect.Method;

public final class OptimizerRefreshTest {
  public static void main(String[] args) throws Exception {
    assertInterval(OptimizerRefreshOption.EVERY_BAR.label, MeridianOptions.FAST, false, 20, 1);
    assertInterval(OptimizerRefreshOption.EVERY_N_BARS.label, MeridianOptions.DEEP, false, 3, 3);
    assertInterval(OptimizerRefreshOption.ON_DEMAND.label, MeridianOptions.FAST, false, 20, Integer.MAX_VALUE);
    assertInterval(OptimizerRefreshOption.ON_DEMAND.label, MeridianOptions.FAST, true, 20, 8);

    assertEquals(180, MeridianOptimizer.maxCandidates(MeridianOptions.FAST), "fast candidate cap");
    assertEquals(500, MeridianOptimizer.maxCandidates(MeridianOptions.MEDIUM), "medium candidate cap");
    assertEquals(1200, MeridianOptimizer.maxCandidates(MeridianOptions.DEEP), "deep candidate cap");

    SettingsView aroundCurrent = cfg(OptimizerRefreshOption.EVERY_N_BARS.label, MeridianOptions.DEEP, false, 20);
    aroundCurrent.optimizerSearch = MeridianOptions.AROUND_CURRENT;
    assertEquals(1, MeridianOptimizer.optimizerPasses(aroundCurrent), "around-current passes");

    SettingsView deep = cfg(OptimizerRefreshOption.EVERY_N_BARS.label, MeridianOptions.DEEP, false, 20);
    deep.optimizerSearch = MeridianOptions.NQ_5_15M_FAST;
    assertEquals(3, MeridianOptimizer.optimizerPasses(deep), "deep passes");
  }

  private static void assertInterval(String mode, String depth, boolean autoApply, int everyN, int expected) throws Exception {
    int actual = refreshInterval(cfg(mode, depth, autoApply, everyN));
    assertEquals(expected, actual, mode + "/" + depth + " interval");
  }

  private static SettingsView cfg(String mode, String depth, boolean autoApply, int everyN) {
    SettingsView cfg = new SettingsView();
    cfg.optRefreshMode = mode;
    cfg.optimizerDepth = depth;
    cfg.autoApplyOptimizer = autoApply;
    cfg.optRefreshInterval = everyN;
    cfg.optimizerSearch = MeridianOptions.NQ_5_15M_FAST;
    return cfg;
  }

  private static int refreshInterval(SettingsView cfg) throws Exception {
    Method method = MeridianOptimizer.class.getDeclaredMethod("refreshInterval", SettingsView.class);
    method.setAccessible(true);
    return (Integer)method.invoke(new MeridianOptimizer(), cfg);
  }

  private static void assertEquals(int expected, int actual, String message) {
    if (expected != actual) throw new AssertionError(message + ": expected " + expected + ", got " + actual);
  }
}
