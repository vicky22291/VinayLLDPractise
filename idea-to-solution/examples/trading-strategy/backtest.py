"""
Backtesting Infrastructure (READ-ONLY)
=======================================
This file is the equivalent of Karpathy's prepare.py — the agent should NOT edit it.

Provides:
  - OHLCV data generation (synthetic for demo, swap for real data)
  - Indicator computation library
  - Backtesting engine with realistic constraints (slippage, commissions)
  - Performance metrics (Sharpe, max drawdown, win rate, etc.)
"""

import datetime
import hashlib
import math
import random
from dataclasses import dataclass


# ---------------------------------------------------------------------------
# Data structures
# ---------------------------------------------------------------------------
@dataclass
class Bar:
    """One OHLCV bar."""

    timestamp: datetime.datetime
    open: float
    high: float
    low: float
    close: float
    volume: float


@dataclass
class Signal:
    """A trading signal produced by a strategy."""

    position: float  # -1.0 (full short) to +1.0 (full long), 0 = flat


@dataclass
class Trade:
    entry_time: datetime.datetime
    exit_time: datetime.datetime
    entry_price: float
    exit_price: float
    direction: float  # +1 or -1
    pnl_pct: float


@dataclass
class BacktestResult:
    """Complete backtest output with all metrics."""

    total_return: float
    sharpe_ratio: float
    max_drawdown: float
    win_rate: float
    total_trades: int
    profit_factor: float
    avg_trade_pnl: float
    max_consecutive_losses: int
    exposure_pct: float  # % of time in market
    equity_curve: list[float]


# ---------------------------------------------------------------------------
# Synthetic data generator (deterministic for reproducibility)
# ---------------------------------------------------------------------------
def generate_ohlcv_data(
    symbol: str = "SYNTH",
    days: int = 500,
    seed: int = 42,
    trend_strength: float = 0.0002,
    volatility: float = 0.015,
    mean_reversion: float = 0.01,
) -> list[Bar]:
    """Generate synthetic daily OHLCV data with realistic properties.

    Properties:
      - Slight upward drift (trend_strength)
      - Volatility clustering (GARCH-like)
      - Mean reversion around a slow-moving trend
      - Volume correlated with absolute returns
    """
    rng = random.Random(seed)
    bars: list[Bar] = []
    price = 100.0
    vol = volatility
    base_volume = 1_000_000.0
    trend_price = price

    start_date = datetime.datetime(2020, 1, 1)

    for day in range(days):
        # Volatility clustering
        vol = 0.9 * vol + 0.1 * volatility * (1 + 2 * abs(rng.gauss(0, 1)))

        # Mean reversion + trend
        trend_price *= 1 + trend_strength
        reversion = mean_reversion * (trend_price - price) / price
        daily_return = reversion + rng.gauss(0, vol)

        open_price = price
        close_price = price * (1 + daily_return)

        # Intraday high/low
        intraday_vol = abs(daily_return) + vol * abs(rng.gauss(0, 0.5))
        high_price = max(open_price, close_price) * (1 + intraday_vol * 0.5)
        low_price = min(open_price, close_price) * (1 - intraday_vol * 0.5)

        # Volume correlates with |return|
        volume = base_volume * (1 + 5 * abs(daily_return) / vol) * (0.8 + 0.4 * rng.random())

        bars.append(
            Bar(
                timestamp=start_date + datetime.timedelta(days=day),
                open=round(open_price, 4),
                high=round(high_price, 4),
                low=round(low_price, 4),
                close=round(close_price, 4),
                volume=round(volume),
            )
        )
        price = close_price

    return bars


# ---------------------------------------------------------------------------
# Indicator library — common technical indicators
# ---------------------------------------------------------------------------
class Indicators:
    """Stateless indicator computations. All return lists aligned with input bars."""

    @staticmethod
    def sma(closes: list[float], period: int) -> list[float | None]:
        result: list[float | None] = [None] * len(closes)
        for i in range(period - 1, len(closes)):
            result[i] = sum(closes[i - period + 1 : i + 1]) / period
        return result

    @staticmethod
    def ema(closes: list[float], period: int) -> list[float | None]:
        result: list[float | None] = [None] * len(closes)
        if len(closes) < period:
            return result
        multiplier = 2 / (period + 1)
        # Seed with SMA
        result[period - 1] = sum(closes[:period]) / period
        for i in range(period, len(closes)):
            prev = result[i - 1]
            if prev is not None:
                result[i] = (closes[i] - prev) * multiplier + prev
        return result

    @staticmethod
    def rsi(closes: list[float], period: int = 14) -> list[float | None]:
        result: list[float | None] = [None] * len(closes)
        if len(closes) < period + 1:
            return result

        gains = []
        losses = []
        for i in range(1, len(closes)):
            change = closes[i] - closes[i - 1]
            gains.append(max(change, 0))
            losses.append(max(-change, 0))

        avg_gain = sum(gains[:period]) / period
        avg_loss = sum(losses[:period]) / period

        for i in range(period, len(closes)):
            if avg_loss == 0:
                result[i] = 100.0
            else:
                rs = avg_gain / avg_loss
                result[i] = 100 - (100 / (1 + rs))

            if i < len(gains):
                avg_gain = (avg_gain * (period - 1) + gains[i]) / period
                avg_loss = (avg_loss * (period - 1) + losses[i]) / period

        return result

    @staticmethod
    def bollinger_bands(
        closes: list[float], period: int = 20, num_std: float = 2.0
    ) -> tuple[list[float | None], list[float | None], list[float | None]]:
        """Returns (upper, middle, lower) bands."""
        middle = Indicators.sma(closes, period)
        upper: list[float | None] = [None] * len(closes)
        lower: list[float | None] = [None] * len(closes)

        for i in range(period - 1, len(closes)):
            if middle[i] is not None:
                window = closes[i - period + 1 : i + 1]
                std = (sum((x - middle[i]) ** 2 for x in window) / period) ** 0.5
                upper[i] = middle[i] + num_std * std
                lower[i] = middle[i] - num_std * std

        return upper, middle, lower

    @staticmethod
    def macd(
        closes: list[float],
        fast: int = 12,
        slow: int = 26,
        signal_period: int = 9,
    ) -> tuple[list[float | None], list[float | None], list[float | None]]:
        """Returns (macd_line, signal_line, histogram)."""
        fast_ema = Indicators.ema(closes, fast)
        slow_ema = Indicators.ema(closes, slow)
        macd_line: list[float | None] = [None] * len(closes)

        for i in range(len(closes)):
            if fast_ema[i] is not None and slow_ema[i] is not None:
                macd_line[i] = fast_ema[i] - slow_ema[i]

        macd_values = [v if v is not None else 0 for v in macd_line]
        signal_line = Indicators.ema(macd_values, signal_period)

        histogram: list[float | None] = [None] * len(closes)
        for i in range(len(closes)):
            if macd_line[i] is not None and signal_line[i] is not None:
                histogram[i] = macd_line[i] - signal_line[i]

        return macd_line, signal_line, histogram

    @staticmethod
    def atr(bars: list[Bar], period: int = 14) -> list[float | None]:
        """Average True Range."""
        result: list[float | None] = [None] * len(bars)
        if len(bars) < 2:
            return result

        true_ranges: list[float] = []
        for i in range(1, len(bars)):
            tr = max(
                bars[i].high - bars[i].low,
                abs(bars[i].high - bars[i - 1].close),
                abs(bars[i].low - bars[i - 1].close),
            )
            true_ranges.append(tr)

        if len(true_ranges) < period:
            return result

        atr_val = sum(true_ranges[:period]) / period
        result[period] = atr_val

        for i in range(period, len(true_ranges)):
            atr_val = (atr_val * (period - 1) + true_ranges[i]) / period
            result[i + 1] = atr_val

        return result

    @staticmethod
    def stochastic(
        bars: list[Bar], k_period: int = 14, d_period: int = 3
    ) -> tuple[list[float | None], list[float | None]]:
        """Stochastic oscillator. Returns (%K, %D)."""
        k_values: list[float | None] = [None] * len(bars)

        for i in range(k_period - 1, len(bars)):
            window = bars[i - k_period + 1 : i + 1]
            highest = max(b.high for b in window)
            lowest = min(b.low for b in window)
            if highest != lowest:
                k_values[i] = 100 * (bars[i].close - lowest) / (highest - lowest)
            else:
                k_values[i] = 50.0

        # %D is SMA of %K
        d_values: list[float | None] = [None] * len(bars)
        for i in range(k_period - 1 + d_period - 1, len(bars)):
            window = k_values[i - d_period + 1 : i + 1]
            valid = [v for v in window if v is not None]
            if len(valid) == d_period:
                d_values[i] = sum(valid) / d_period

        return k_values, d_values

    @staticmethod
    def adx(bars: list[Bar], period: int = 14) -> list[float | None]:
        """Average Directional Index."""
        result: list[float | None] = [None] * len(bars)
        if len(bars) < period * 2:
            return result

        plus_dm_list: list[float] = []
        minus_dm_list: list[float] = []
        tr_list: list[float] = []

        for i in range(1, len(bars)):
            high_diff = bars[i].high - bars[i - 1].high
            low_diff = bars[i - 1].low - bars[i].low

            plus_dm = high_diff if high_diff > low_diff and high_diff > 0 else 0
            minus_dm = low_diff if low_diff > high_diff and low_diff > 0 else 0

            tr = max(
                bars[i].high - bars[i].low,
                abs(bars[i].high - bars[i - 1].close),
                abs(bars[i].low - bars[i - 1].close),
            )

            plus_dm_list.append(plus_dm)
            minus_dm_list.append(minus_dm)
            tr_list.append(tr)

        if len(tr_list) < period:
            return result

        # Smoothed values
        smooth_plus = sum(plus_dm_list[:period])
        smooth_minus = sum(minus_dm_list[:period])
        smooth_tr = sum(tr_list[:period])

        dx_values: list[float] = []

        for i in range(period - 1, len(tr_list)):
            if i > period - 1:
                smooth_plus = smooth_plus - smooth_plus / period + plus_dm_list[i]
                smooth_minus = smooth_minus - smooth_minus / period + minus_dm_list[i]
                smooth_tr = smooth_tr - smooth_tr / period + tr_list[i]

            if smooth_tr > 0:
                plus_di = 100 * smooth_plus / smooth_tr
                minus_di = 100 * smooth_minus / smooth_tr
                if plus_di + minus_di > 0:
                    dx = 100 * abs(plus_di - minus_di) / (plus_di + minus_di)
                    dx_values.append(dx)
                else:
                    dx_values.append(0)
            else:
                dx_values.append(0)

        if len(dx_values) >= period:
            adx_val = sum(dx_values[:period]) / period
            idx = 2 * period
            if idx < len(result):
                result[idx] = adx_val
            for i in range(period, len(dx_values)):
                adx_val = (adx_val * (period - 1) + dx_values[i]) / period
                idx = i + period
                if idx < len(result):
                    result[idx] = adx_val

        return result


# ---------------------------------------------------------------------------
# Backtesting engine
# ---------------------------------------------------------------------------
class BacktestEngine:
    """Simulates strategy execution with realistic constraints."""

    def __init__(
        self,
        commission_pct: float = 0.001,  # 0.1% per trade (10 bps)
        slippage_pct: float = 0.0005,  # 0.05% slippage
        initial_capital: float = 100_000.0,
    ):
        self.commission_pct = commission_pct
        self.slippage_pct = slippage_pct
        self.initial_capital = initial_capital

    def run(
        self, bars: list[Bar], signals: list[Signal]
    ) -> BacktestResult:
        """Run backtest given bars and corresponding signals."""
        assert len(bars) == len(signals), (
            f"bars ({len(bars)}) and signals ({len(signals)}) must have same length"
        )

        equity = self.initial_capital
        equity_curve = [equity]
        trades: list[Trade] = []
        current_position = 0.0
        entry_price = 0.0
        entry_time = bars[0].timestamp
        peak_equity = equity

        for i in range(1, len(bars)):
            target_position = max(-1.0, min(1.0, signals[i].position))

            # Position change
            if abs(target_position - current_position) > 0.01:
                # Close existing position
                if abs(current_position) > 0.01:
                    exit_price = bars[i].open * (
                        1 - self.slippage_pct * (1 if current_position > 0 else -1)
                    )
                    pnl_pct = current_position * (exit_price - entry_price) / entry_price
                    pnl_pct -= self.commission_pct  # commission on exit
                    equity *= 1 + pnl_pct

                    trades.append(
                        Trade(
                            entry_time=entry_time,
                            exit_time=bars[i].timestamp,
                            entry_price=entry_price,
                            exit_price=exit_price,
                            direction=1 if current_position > 0 else -1,
                            pnl_pct=pnl_pct,
                        )
                    )

                # Open new position
                if abs(target_position) > 0.01:
                    entry_price = bars[i].open * (
                        1 + self.slippage_pct * (1 if target_position > 0 else -1)
                    )
                    entry_time = bars[i].timestamp
                    equity *= 1 - self.commission_pct  # commission on entry

                current_position = target_position
            else:
                # Hold — mark to market for equity curve
                if abs(current_position) > 0.01:
                    mtm_pnl = current_position * (
                        bars[i].close - bars[i - 1].close
                    ) / bars[i - 1].close
                    equity *= 1 + mtm_pnl

            equity_curve.append(equity)
            peak_equity = max(peak_equity, equity)

        # Close any remaining position
        if abs(current_position) > 0.01 and len(bars) > 1:
            exit_price = bars[-1].close
            pnl_pct = current_position * (exit_price - entry_price) / entry_price
            pnl_pct -= self.commission_pct
            equity *= 1 + pnl_pct
            equity_curve[-1] = equity
            trades.append(
                Trade(
                    entry_time=entry_time,
                    exit_time=bars[-1].timestamp,
                    entry_price=entry_price,
                    exit_price=exit_price,
                    direction=1 if current_position > 0 else -1,
                    pnl_pct=pnl_pct,
                )
            )

        return self._compute_metrics(equity_curve, trades)

    def _compute_metrics(
        self, equity_curve: list[float], trades: list[Trade]
    ) -> BacktestResult:
        # Total return
        total_return = (equity_curve[-1] - equity_curve[0]) / equity_curve[0]

        # Daily returns for Sharpe
        daily_returns = []
        for i in range(1, len(equity_curve)):
            daily_returns.append(
                (equity_curve[i] - equity_curve[i - 1]) / equity_curve[i - 1]
            )

        # Sharpe ratio (annualized, assuming 252 trading days)
        if daily_returns and len(daily_returns) > 1:
            mean_ret = sum(daily_returns) / len(daily_returns)
            std_ret = (
                sum((r - mean_ret) ** 2 for r in daily_returns) / (len(daily_returns) - 1)
            ) ** 0.5
            sharpe_ratio = (mean_ret / std_ret * math.sqrt(252)) if std_ret > 0 else 0
        else:
            sharpe_ratio = 0.0

        # Max drawdown
        peak = equity_curve[0]
        max_dd = 0.0
        for eq in equity_curve:
            peak = max(peak, eq)
            dd = (peak - eq) / peak
            max_dd = max(max_dd, dd)

        # Win rate
        if trades:
            wins = sum(1 for t in trades if t.pnl_pct > 0)
            win_rate = wins / len(trades)

            gross_profit = sum(t.pnl_pct for t in trades if t.pnl_pct > 0)
            gross_loss = abs(sum(t.pnl_pct for t in trades if t.pnl_pct < 0))
            profit_factor = gross_profit / gross_loss if gross_loss > 0 else float("inf")

            avg_pnl = sum(t.pnl_pct for t in trades) / len(trades)

            # Max consecutive losses
            max_consec = 0
            current_consec = 0
            for t in trades:
                if t.pnl_pct < 0:
                    current_consec += 1
                    max_consec = max(max_consec, current_consec)
                else:
                    current_consec = 0
        else:
            win_rate = 0.0
            profit_factor = 0.0
            avg_pnl = 0.0
            max_consec = 0

        # Exposure (% of bars with a position)
        # Approximate from trades
        total_bars = len(equity_curve)
        in_market_bars = sum(
            max(1, (t.exit_time - t.entry_time).days) for t in trades
        ) if trades else 0
        exposure_pct = min(1.0, in_market_bars / total_bars) if total_bars > 0 else 0

        return BacktestResult(
            total_return=round(total_return, 6),
            sharpe_ratio=round(sharpe_ratio, 4),
            max_drawdown=round(max_dd, 6),
            win_rate=round(win_rate, 4),
            total_trades=len(trades),
            profit_factor=round(profit_factor, 4),
            avg_trade_pnl=round(avg_pnl, 6),
            max_consecutive_losses=max_consec,
            exposure_pct=round(exposure_pct, 4),
            equity_curve=equity_curve,
        )


# ---------------------------------------------------------------------------
# Data splits — train/validation/test
# ---------------------------------------------------------------------------
def split_data(
    bars: list[Bar],
    train_pct: float = 0.6,
    val_pct: float = 0.2,
) -> tuple[list[Bar], list[Bar], list[Bar]]:
    """Split bars into train/validation/test sets chronologically."""
    n = len(bars)
    train_end = int(n * train_pct)
    val_end = int(n * (train_pct + val_pct))
    return bars[:train_end], bars[train_end:val_end], bars[val_end:]
