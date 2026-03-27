"""
Trading Strategy (AGENT-EDITABLE)
==================================
This is the file the AI agent modifies during experiments.
Equivalent to Karpathy's train.py.

The agent should:
  - Try different indicator combinations
  - Adjust parameters (periods, thresholds)
  - Add/remove entry/exit conditions
  - Experiment with position sizing
  - Try different regime filters

Rules:
  - Must implement generate_signals(bars) -> list[Signal]
  - Must return exactly len(bars) signals
  - Signal.position must be in [-1.0, 1.0]
  - Can import from backtest.py (Indicators, Bar, Signal)
"""

from backtest import Bar, Indicators, Signal


def generate_signals(bars: list[Bar]) -> list[Signal]:
    """Generate trading signals for each bar.

    Current strategy: Simple SMA crossover
      - Long when fast SMA > slow SMA
      - Flat otherwise

    This is the baseline. The agent should improve it.
    """
    closes = [b.close for b in bars]

    # Simple moving average crossover
    fast_sma = Indicators.sma(closes, period=10)
    slow_sma = Indicators.sma(closes, period=30)

    signals: list[Signal] = []
    for i in range(len(bars)):
        if fast_sma[i] is None or slow_sma[i] is None:
            signals.append(Signal(position=0.0))
        elif fast_sma[i] > slow_sma[i]:
            signals.append(Signal(position=1.0))
        else:
            signals.append(Signal(position=0.0))

    return signals
