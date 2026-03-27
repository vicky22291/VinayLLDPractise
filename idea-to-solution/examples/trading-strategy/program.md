# Trading Strategy Optimization: Agent Instructions

## Your Mission

You are an autonomous trading strategy researcher. Your goal is to maximize the
**Sharpe ratio** on the validation set by iteratively improving `strategy.py`.

## Domain Context

You have:
- **backtest.py** (READ-ONLY): Synthetic OHLCV data, indicator library, backtesting engine
- **strategy.py** (YOU EDIT THIS): Trading strategy that produces signals
- **run.py** (READ-ONLY): Evaluator that runs strategy and prints metrics

## Available Indicators (in backtest.py)

| Indicator | Method | Parameters |
|-----------|--------|------------|
| SMA | `Indicators.sma(closes, period)` | period |
| EMA | `Indicators.ema(closes, period)` | period |
| RSI | `Indicators.rsi(closes, period)` | period (default 14) |
| Bollinger Bands | `Indicators.bollinger_bands(closes, period, num_std)` | period, num_std |
| MACD | `Indicators.macd(closes, fast, slow, signal)` | fast, slow, signal periods |
| ATR | `Indicators.atr(bars, period)` | period (default 14) |
| Stochastic | `Indicators.stochastic(bars, k_period, d_period)` | k_period, d_period |
| ADX | `Indicators.adx(bars, period)` | period (default 14) |

## Experiment Ideas (Ordered by Likely Impact)

### Phase 1: Signal Quality
1. Try EMA crossover instead of SMA crossover (faster response)
2. Add RSI filter (only enter when RSI confirms trend)
3. Try MACD crossover as primary signal
4. Combine trend + momentum indicators

### Phase 2: Risk Management
5. Add ATR-based position sizing (scale position by inverse volatility)
6. Add maximum drawdown circuit breaker
7. Use Bollinger Bands for mean-reversion overlay
8. Filter trades by ADX (only trade in trending markets)

### Phase 3: Advanced Combinations
9. Multi-timeframe: use different indicator periods for entry vs. exit
10. Regime detection: switch between trend-following and mean-reversion
11. Add volume confirmation
12. Optimize parameter values (SMA periods, RSI thresholds)

### Phase 4: Refinement
13. Reduce trade frequency (fewer but higher-quality trades)
14. Add pyramiding (scale into winning positions)
15. Experiment with short-selling conditions
16. Simplify — remove indicators that don't contribute

## Constraints

- **Sharpe ratio is the primary metric.** Total return is secondary.
- Watch for **overfitting**: if `sharpe_decay` > 0.5, the strategy is likely overfit.
- **Max drawdown** should stay under 30%. Reject strategies with > 30% drawdown.
- Keep the strategy **readable**. No obfuscated parameter arrays.
- Prefer **fewer indicators** over more. Each indicator must earn its place.

## The Loop

```
FOREVER:
  1. Read results.tsv — what's been tried, what worked
  2. Form a hypothesis (one specific change)
  3. Edit strategy.py (one change only)
  4. git commit
  5. python run.py
  6. Read sharpe_ratio from output
  7. Keep if improved AND drawdown < 30%, else discard
  8. Log to results.tsv
  9. GOTO 1
```

## Output Format

After each experiment, report:
```
EXP-{id}: {status} | sharpe: {before} → {after} | hypothesis: {description}
```
