# Meridian Flow Forge

Meridian Flow Forge is a MotiveWave SDK study for market-structure signals, order-block visualization, risk projections, dashboard metrics, and advisory parameter optimization.

## Features

- BOS/CHoCH market-structure detection.
- Suggested order blocks with optional mitigation handling.
- Forge filters including SMA/EMA, RSI, MACD, Supertrend, Stochastic, Bollinger trend, AO, SAR, CCI, ADX, Tilson IE2, and SMI Ergodic.
- ATR-based risk lines, targets, and live projection overlay.
- On-chart dashboard with win rate, profit factor, net R, drawdown, and optimizer notes.
- Optional optimizer for current chart/instrument/period settings.

## Build

Requires Java 26 and the MotiveWave SDK jar.

```bash
mkdir -p build/classes
/usr/lib/jvm/java-26-openjdk/bin/javac \
  -cp /home/user/.local/opt/motivewave-7.0.26/usr/share/motivewave/jar/mwave_sdk.jar \
  -d build/classes \
  src/mwext/meridian/*.java
/usr/lib/jvm/java-26-openjdk/bin/jar cf build/meridian-flow-forge.jar -C build/classes .
```

## Install

Copy the built jar into MotiveWave's extension directory and restart MotiveWave:

```bash
cp build/meridian-flow-forge.jar "$HOME/MotiveWave Extensions/meridian-flow-forge.jar"
```

Then add the study from the `tdawe` menu as **Meridian Flow Forge**.
