"""
Run Script — Evaluates the current strategy and prints metrics.
================================================================
This is what the harness calls for each experiment.
Prints metrics in a parseable format that the harness extracts.
"""

import json
import sys

from backtest import BacktestEngine, generate_ohlcv_data, split_data
from strategy import generate_signals


def main():
    # Generate data (deterministic)
    bars = generate_ohlcv_data(days=500, seed=42)
    train_bars, val_bars, test_bars = split_data(bars)

    # Run strategy on validation set (what we optimize against)
    val_signals = generate_signals(val_bars)
    engine = BacktestEngine()
    val_result = engine.run(val_bars, val_signals)

    # Also run on train set for overfitting detection
    train_signals = generate_signals(train_bars)
    train_result = engine.run(train_bars, train_signals)

    # Print results in parseable format
    print("=" * 50)
    print("EVALUATION RESULTS")
    print("=" * 50)
    print(f"sharpe_ratio: {val_result.sharpe_ratio}")
    print(f"total_return: {val_result.total_return}")
    print(f"max_drawdown: {val_result.max_drawdown}")
    print(f"win_rate: {val_result.win_rate}")
    print(f"total_trades: {val_result.total_trades}")
    print(f"profit_factor: {val_result.profit_factor}")
    print(f"avg_trade_pnl: {val_result.avg_trade_pnl}")
    print(f"max_consecutive_losses: {val_result.max_consecutive_losses}")
    print(f"exposure_pct: {val_result.exposure_pct}")
    print()
    print("--- Overfitting Check ---")
    print(f"train_sharpe: {train_result.sharpe_ratio}")
    print(f"val_sharpe: {val_result.sharpe_ratio}")
    sharpe_decay = (
        (train_result.sharpe_ratio - val_result.sharpe_ratio) / abs(train_result.sharpe_ratio)
        if train_result.sharpe_ratio != 0
        else 0
    )
    print(f"sharpe_decay: {round(sharpe_decay, 4)}")
    print()
    print("--- JSON (for programmatic access) ---")
    print(
        json.dumps(
            {
                "sharpe_ratio": val_result.sharpe_ratio,
                "total_return": val_result.total_return,
                "max_drawdown": val_result.max_drawdown,
                "win_rate": val_result.win_rate,
                "total_trades": val_result.total_trades,
                "profit_factor": val_result.profit_factor,
                "train_sharpe": train_result.sharpe_ratio,
                "sharpe_decay": round(sharpe_decay, 4),
            }
        )
    )


if __name__ == "__main__":
    main()
