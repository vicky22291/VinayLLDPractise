# TradingAgents - Usage and Integration Guide

## Quick Start

### Basic Usage Example

```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

# Initialize the framework
ta = TradingAgentsGraph(debug=True, config=DEFAULT_CONFIG.copy())

# Analyze a stock on a specific date
final_state, decision = ta.propagate("NVDA", "2024-05-10")

# Get the final decision
print(f"Decision: {decision}")  # Output: "BUY", "SELL", or "HOLD"

# Access detailed reports
print(f"\nMarket Analysis:\n{final_state['market_report']}")
print(f"\nFundamentals:\n{final_state['fundamentals_report']}")
print(f"\nFinal Decision:\n{final_state['final_decision']}")
```

### CLI Usage

```bash
# Run interactive CLI
python -m cli.main

# Follow prompts to:
# 1. Select ticker symbol
# 2. Choose analysis date
# 3. Configure LLM settings
# 4. Select research depth
# 5. View real-time analysis progress
```

---

## Core Usage Patterns

### 1. Single Stock Analysis

```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG
import json

def analyze_stock(ticker, date, config=None):
    """Analyze a single stock"""
    if config is None:
        config = DEFAULT_CONFIG.copy()

    # Initialize
    ta = TradingAgentsGraph(
        selected_analysts=["market", "fundamentals", "news", "social"],
        debug=True,
        config=config
    )

    # Analyze
    final_state, decision = ta.propagate(ticker, date)

    # Return structured results
    return {
        "ticker": ticker,
        "date": date,
        "decision": decision,
        "market_analysis": final_state["market_report"],
        "fundamental_analysis": final_state["fundamentals_report"],
        "news_analysis": final_state["news_report"],
        "sentiment_analysis": final_state["sentiment_report"],
        "investment_plan": final_state["investment_debate_state"]["judge_decision"],
        "final_decision": final_state["final_decision"]
    }

# Usage
result = analyze_stock("AAPL", "2024-05-15")
print(json.dumps(result, indent=2))
```

### 2. Multi-Stock Screening

```python
import concurrent.futures
from datetime import datetime

def screen_stocks(tickers, date=None, max_workers=5):
    """Screen multiple stocks in parallel"""
    if date is None:
        date = datetime.now().strftime("%Y-%m-%d")

    # Use quick config for screening
    config = DEFAULT_CONFIG.copy()
    config["quick_think_llm"] = "gpt-4o-mini"
    config["max_debate_rounds"] = 0  # Skip debate for speed
    config["max_risk_discuss_rounds"] = 0

    def analyze_single(ticker):
        try:
            ta = TradingAgentsGraph(
                selected_analysts=["market", "fundamentals"],  # Minimal set
                debug=False,
                config=config
            )
            final_state, decision = ta.propagate(ticker, date)
            return {
                "ticker": ticker,
                "decision": decision,
                "success": True
            }
        except Exception as e:
            return {
                "ticker": ticker,
                "decision": "ERROR",
                "error": str(e),
                "success": False
            }

    # Parallel execution
    with concurrent.futures.ThreadPoolExecutor(max_workers=max_workers) as executor:
        results = list(executor.map(analyze_single, tickers))

    # Filter and return
    buy_signals = [r for r in results if r["decision"] == "BUY" and r["success"]]
    sell_signals = [r for r in results if r["decision"] == "SELL" and r["success"]]
    hold_signals = [r for r in results if r["decision"] == "HOLD" and r["success"]]

    return {
        "buy": buy_signals,
        "sell": sell_signals,
        "hold": hold_signals,
        "errors": [r for r in results if not r["success"]]
    }

# Usage
watchlist = ["AAPL", "MSFT", "GOOGL", "AMZN", "NVDA", "TSLA"]
screening_results = screen_stocks(watchlist)

print(f"BUY signals: {len(screening_results['buy'])}")
for stock in screening_results['buy']:
    print(f"  - {stock['ticker']}")
```

### 3. Daily Analysis with Scheduling

```python
import schedule
import time
from datetime import datetime

def daily_market_analysis():
    """Run daily before market open"""
    today = datetime.now().strftime("%Y-%m-%d")

    # Your watchlist
    tickers = ["AAPL", "MSFT", "NVDA", "TSLA"]

    print(f"\n=== Daily Analysis for {today} ===\n")

    for ticker in tickers:
        try:
            result = analyze_stock(ticker, today)
            print(f"{ticker}: {result['decision']}")

            # Save to file
            with open(f"analysis_{ticker}_{today}.json", "w") as f:
                json.dump(result, f, indent=2)

        except Exception as e:
            print(f"{ticker}: ERROR - {e}")

    print("\nDaily analysis complete!")

# Schedule daily at 8:00 AM (before market open)
schedule.every().day.at("08:00").do(daily_market_analysis)

# Run scheduler
while True:
    schedule.run_pending()
    time.sleep(60)  # Check every minute
```

### 4. Backtesting Framework

```python
from datetime import datetime, timedelta
import pandas as pd

def backtest_strategy(ticker, start_date, end_date, rebalance_days=5):
    """Backtest trading strategy over historical period"""
    current_date = datetime.strptime(start_date, "%Y-%m-%d")
    end = datetime.strptime(end_date, "%Y-%m-%d")

    trades = []
    position = None  # None, "LONG", "SHORT"
    entry_price = None

    while current_date <= end:
        date_str = current_date.strftime("%Y-%m-%d")

        try:
            # Analyze on this date
            result = analyze_stock(ticker, date_str)
            decision = result["decision"]

            # Trading logic
            if decision == "BUY" and position != "LONG":
                # Enter long position
                if position == "SHORT":
                    # Close short first
                    trades.append({
                        "date": date_str,
                        "action": "COVER",
                        "ticker": ticker,
                        "price": get_price(ticker, date_str)
                    })

                # Open long
                entry_price = get_price(ticker, date_str)
                position = "LONG"
                trades.append({
                    "date": date_str,
                    "action": "BUY",
                    "ticker": ticker,
                    "price": entry_price
                })

            elif decision == "SELL" and position != "SHORT":
                # Enter short position
                if position == "LONG":
                    # Close long first
                    trades.append({
                        "date": date_str,
                        "action": "SELL",
                        "ticker": ticker,
                        "price": get_price(ticker, date_str)
                    })

                # Open short
                entry_price = get_price(ticker, date_str)
                position = "SHORT"
                trades.append({
                    "date": date_str,
                    "action": "SHORT",
                    "ticker": ticker,
                    "price": entry_price
                })

            elif decision == "HOLD" and position is not None:
                # Hold current position
                pass

        except Exception as e:
            print(f"Error on {date_str}: {e}")

        # Move to next rebalance date
        current_date += timedelta(days=rebalance_days)

    # Calculate returns
    df = pd.DataFrame(trades)
    return analyze_backtest_results(df)

def get_price(ticker, date):
    """Get actual stock price on date"""
    import yfinance as yf
    stock = yf.Ticker(ticker)
    hist = stock.history(start=date, end=date)
    if len(hist) > 0:
        return hist['Close'].iloc[0]
    return None

def analyze_backtest_results(trades_df):
    """Calculate backtest metrics"""
    # Implementation: calculate total return, Sharpe ratio, max drawdown, etc.
    pass
```

### 5. Learning from Outcomes (Reflection)

```python
def trading_workflow_with_learning(ticker, date):
    """Full workflow: analyze → execute → reflect → learn"""

    # Step 1: Initial Analysis
    ta = TradingAgentsGraph(debug=True, config=DEFAULT_CONFIG.copy())
    final_state, decision = ta.propagate(ticker, date)

    print(f"Initial Decision: {decision}")

    # Step 2: Execute Trade (simulated)
    entry_price = get_current_price(ticker)
    print(f"Entry Price: ${entry_price}")

    # Step 3: Wait for outcome (in reality, wait days/weeks)
    # For demo, get price after N days
    import time
    time.sleep(10)  # In reality, this would be days
    exit_price = get_current_price(ticker)

    # Calculate return
    actual_return = (exit_price - entry_price) / entry_price
    print(f"Actual Return: {actual_return:.2%}")

    # Step 4: Reflect and Learn
    reflection_data = {
        "ticker": ticker,
        "actual_return": actual_return,
        "decision": decision,
        "components": {
            "bull_argument": final_state["investment_debate_state"]["bull_history"],
            "bear_argument": final_state["investment_debate_state"]["bear_history"],
            "trader_decision": final_state["final_decision"],
            "research_judge": final_state["investment_debate_state"]["judge_decision"],
            "risk_manager": final_state["final_decision"]
        }
    }

    # Update memories
    ta.reflect_and_remember(reflection_data)

    print("Memories updated for future learning!")

    return {
        "decision": decision,
        "entry_price": entry_price,
        "exit_price": exit_price,
        "return": actual_return
    }

# Usage
result = trading_workflow_with_learning("NVDA", "2024-05-10")
```

---

## Integration Patterns

### 1. REST API Integration

```python
from flask import Flask, request, jsonify
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

app = Flask(__name__)

# Initialize on startup
ta_instance = None

@app.before_first_request
def initialize():
    global ta_instance
    config = DEFAULT_CONFIG.copy()
    config["debug"] = False
    ta_instance = TradingAgentsGraph(
        selected_analysts=["market", "fundamentals"],
        debug=False,
        config=config
    )

@app.route('/analyze', methods=['POST'])
def analyze():
    """
    POST /analyze
    {
        "ticker": "NVDA",
        "date": "2024-05-10"
    }
    """
    data = request.get_json()
    ticker = data.get("ticker")
    date = data.get("date")

    if not ticker or not date:
        return jsonify({"error": "Missing ticker or date"}), 400

    try:
        final_state, decision = ta_instance.propagate(ticker, date)

        return jsonify({
            "ticker": ticker,
            "date": date,
            "decision": decision,
            "reports": {
                "market": final_state["market_report"],
                "fundamentals": final_state["fundamentals_report"]
            },
            "final_decision": final_state["final_decision"]
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/screen', methods=['POST'])
def screen():
    """
    POST /screen
    {
        "tickers": ["AAPL", "MSFT", "NVDA"],
        "date": "2024-05-10"
    }
    """
    data = request.get_json()
    tickers = data.get("tickers", [])
    date = data.get("date")

    results = screen_stocks(tickers, date)
    return jsonify(results)

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

### 2. Database Integration

```python
import sqlite3
from datetime import datetime

class TradingDatabase:
    def __init__(self, db_path="trading_agents.db"):
        self.conn = sqlite3.connect(db_path)
        self.create_tables()

    def create_tables(self):
        """Create database schema"""
        cursor = self.conn.cursor()

        # Analysis table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS analyses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                ticker TEXT NOT NULL,
                analysis_date TEXT NOT NULL,
                decision TEXT NOT NULL,
                market_report TEXT,
                fundamentals_report TEXT,
                news_report TEXT,
                sentiment_report TEXT,
                final_decision TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)

        # Trades table
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS trades (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                analysis_id INTEGER,
                ticker TEXT NOT NULL,
                action TEXT NOT NULL,
                entry_price REAL,
                exit_price REAL,
                return_pct REAL,
                entry_date TEXT,
                exit_date TEXT,
                FOREIGN KEY (analysis_id) REFERENCES analyses(id)
            )
        """)

        self.conn.commit()

    def save_analysis(self, result):
        """Save analysis to database"""
        cursor = self.conn.cursor()
        cursor.execute("""
            INSERT INTO analyses
            (ticker, analysis_date, decision, market_report, fundamentals_report,
             news_report, sentiment_report, final_decision)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            result["ticker"],
            result["date"],
            result["decision"],
            result.get("market_analysis"),
            result.get("fundamental_analysis"),
            result.get("news_analysis"),
            result.get("sentiment_analysis"),
            result.get("final_decision")
        ))
        self.conn.commit()
        return cursor.lastrowid

    def save_trade(self, analysis_id, trade_data):
        """Save trade execution"""
        cursor = self.conn.cursor()
        cursor.execute("""
            INSERT INTO trades
            (analysis_id, ticker, action, entry_price, exit_price,
             return_pct, entry_date, exit_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            analysis_id,
            trade_data["ticker"],
            trade_data["action"],
            trade_data.get("entry_price"),
            trade_data.get("exit_price"),
            trade_data.get("return_pct"),
            trade_data.get("entry_date"),
            trade_data.get("exit_date")
        ))
        self.conn.commit()

    def get_history(self, ticker, limit=10):
        """Get historical analyses for ticker"""
        cursor = self.conn.cursor()
        cursor.execute("""
            SELECT * FROM analyses
            WHERE ticker = ?
            ORDER BY analysis_date DESC
            LIMIT ?
        """, (ticker, limit))
        return cursor.fetchall()

# Usage
db = TradingDatabase()

# Analyze and save
result = analyze_stock("NVDA", "2024-05-10")
analysis_id = db.save_analysis(result)

# Later, save trade outcome
trade = {
    "ticker": "NVDA",
    "action": "BUY",
    "entry_price": 450.32,
    "exit_price": 475.10,
    "return_pct": 5.5,
    "entry_date": "2024-05-10",
    "exit_date": "2024-05-20"
}
db.save_trade(analysis_id, trade)
```

### 3. Webhook Integration

```python
import requests

def analyze_with_webhook(ticker, date, webhook_url):
    """Analyze stock and send results to webhook"""

    # Perform analysis
    result = analyze_stock(ticker, date)

    # Send to webhook
    payload = {
        "ticker": ticker,
        "date": date,
        "decision": result["decision"],
        "timestamp": datetime.now().isoformat()
    }

    response = requests.post(webhook_url, json=payload)

    if response.status_code == 200:
        print(f"Webhook notification sent for {ticker}")
    else:
        print(f"Webhook failed: {response.status_code}")

    return result

# Usage with Discord webhook
DISCORD_WEBHOOK = "https://discord.com/api/webhooks/YOUR_WEBHOOK"

def send_to_discord(ticker, decision):
    """Send trading signal to Discord"""
    message = {
        "content": f"🤖 **Trading Signal**\n"
                   f"Ticker: **{ticker}**\n"
                   f"Decision: **{decision}**\n"
                   f"Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
    }
    requests.post(DISCORD_WEBHOOK, json=message)

# Usage with Slack webhook
SLACK_WEBHOOK = "https://hooks.slack.com/services/YOUR_WEBHOOK"

def send_to_slack(ticker, decision, details):
    """Send trading signal to Slack"""
    message = {
        "text": f"Trading Signal: {ticker}",
        "blocks": [
            {
                "type": "section",
                "text": {
                    "type": "mrkdwn",
                    "text": f"*{ticker}* - {decision}\n{details}"
                }
            }
        ]
    }
    requests.post(SLACK_WEBHOOK, json=message)
```

### 4. Paper Trading Integration

```python
class PaperTradingEngine:
    def __init__(self, initial_capital=100000):
        self.capital = initial_capital
        self.positions = {}  # {ticker: {shares, avg_price}}
        self.trades = []
        self.ta = TradingAgentsGraph(debug=False, config=DEFAULT_CONFIG.copy())

    def analyze_and_trade(self, ticker, date):
        """Analyze and execute paper trade"""

        # Get analysis
        final_state, decision = self.ta.propagate(ticker, date)

        # Get current price
        price = get_current_price(ticker)

        # Execute based on decision
        if decision == "BUY":
            self.buy(ticker, price, date)
        elif decision == "SELL" and ticker in self.positions:
            self.sell(ticker, price, date)

        return decision

    def buy(self, ticker, price, date, position_size_pct=0.1):
        """Buy shares with % of capital"""
        amount = self.capital * position_size_pct
        shares = int(amount / price)

        if shares > 0:
            cost = shares * price

            if ticker in self.positions:
                # Average up
                existing = self.positions[ticker]
                total_shares = existing["shares"] + shares
                total_cost = (existing["shares"] * existing["avg_price"]) + cost
                avg_price = total_cost / total_shares

                self.positions[ticker] = {
                    "shares": total_shares,
                    "avg_price": avg_price
                }
            else:
                # New position
                self.positions[ticker] = {
                    "shares": shares,
                    "avg_price": price
                }

            self.capital -= cost
            self.trades.append({
                "date": date,
                "ticker": ticker,
                "action": "BUY",
                "shares": shares,
                "price": price
            })

            print(f"BUY: {shares} shares of {ticker} @ ${price:.2f}")

    def sell(self, ticker, price, date):
        """Sell all shares of ticker"""
        if ticker not in self.positions:
            return

        position = self.positions[ticker]
        shares = position["shares"]
        proceeds = shares * price

        # Calculate P&L
        cost_basis = shares * position["avg_price"]
        profit = proceeds - cost_basis
        profit_pct = (profit / cost_basis) * 100

        self.capital += proceeds
        del self.positions[ticker]

        self.trades.append({
            "date": date,
            "ticker": ticker,
            "action": "SELL",
            "shares": shares,
            "price": price,
            "profit": profit,
            "profit_pct": profit_pct
        })

        print(f"SELL: {shares} shares of {ticker} @ ${price:.2f} "
              f"(P&L: ${profit:.2f}, {profit_pct:.2f}%)")

    def get_portfolio_value(self):
        """Calculate total portfolio value"""
        holdings_value = sum(
            pos["shares"] * get_current_price(ticker)
            for ticker, pos in self.positions.items()
        )
        return self.capital + holdings_value

    def get_performance(self):
        """Get performance metrics"""
        current_value = self.get_portfolio_value()
        total_return = ((current_value - 100000) / 100000) * 100

        return {
            "initial_capital": 100000,
            "current_value": current_value,
            "total_return_pct": total_return,
            "cash": self.capital,
            "positions": self.positions,
            "num_trades": len(self.trades)
        }

# Usage
engine = PaperTradingEngine(initial_capital=100000)

watchlist = ["AAPL", "MSFT", "NVDA"]
today = datetime.now().strftime("%Y-%m-%d")

for ticker in watchlist:
    engine.analyze_and_trade(ticker, today)

print(f"\nPortfolio Performance:")
print(json.dumps(engine.get_performance(), indent=2))
```

---

## Advanced Usage Patterns

### 1. Custom Analyst Selection Based on Strategy

```python
def create_strategy_config(strategy_type):
    """Create config optimized for strategy type"""

    config = DEFAULT_CONFIG.copy()

    if strategy_type == "day_trading":
        return {
            **config,
            "quick_think_llm": "gpt-4o-mini",
            "max_debate_rounds": 0,
            "selected_analysts": ["market", "social"]
        }

    elif strategy_type == "value_investing":
        return {
            **config,
            "deep_think_llm": "o1-preview",
            "max_debate_rounds": 3,
            "selected_analysts": ["fundamentals", "news"]
        }

    elif strategy_type == "swing_trading":
        return {
            **config,
            "deep_think_llm": "gpt-4o",
            "max_debate_rounds": 1,
            "selected_analysts": ["market", "fundamentals"]
        }

    elif strategy_type == "momentum":
        return {
            **config,
            "quick_think_llm": "gpt-4o-mini",
            "max_debate_rounds": 1,
            "selected_analysts": ["market", "social", "news"]
        }

# Usage
config = create_strategy_config("swing_trading")
ta = TradingAgentsGraph(
    selected_analysts=config["selected_analysts"],
    debug=True,
    config=config
)
```

### 2. Batch Processing with Progress Tracking

```python
from tqdm import tqdm
import pandas as pd

def batch_analyze(tickers, date, output_file="results.csv"):
    """Analyze multiple stocks with progress bar"""

    results = []

    for ticker in tqdm(tickers, desc="Analyzing stocks"):
        try:
            result = analyze_stock(ticker, date)
            results.append({
                "ticker": ticker,
                "decision": result["decision"],
                "status": "SUCCESS"
            })
        except Exception as e:
            results.append({
                "ticker": ticker,
                "decision": "ERROR",
                "status": str(e)
            })

    # Save to CSV
    df = pd.DataFrame(results)
    df.to_csv(output_file, index=False)

    # Summary
    print(f"\n=== Analysis Summary ===")
    print(f"Total: {len(tickers)}")
    print(f"BUY: {len(df[df['decision'] == 'BUY'])}")
    print(f"SELL: {len(df[df['decision'] == 'SELL'])}")
    print(f"HOLD: {len(df[df['decision'] == 'HOLD'])}")
    print(f"ERROR: {len(df[df['decision'] == 'ERROR'])}")

    return df

# Usage with S&P 500
sp500_tickers = pd.read_html(
    'https://en.wikipedia.org/wiki/List_of_S%26P_500_companies'
)[0]['Symbol'].tolist()

results_df = batch_analyze(sp500_tickers[:50], "2024-05-10")
```

### 3. Alert System

```python
class TradingAlertSystem:
    def __init__(self, email_config=None, sms_config=None):
        self.email_config = email_config
        self.sms_config = sms_config

    def analyze_and_alert(self, ticker, date, alert_on=["BUY", "SELL"]):
        """Analyze and send alerts for specified signals"""

        result = analyze_stock(ticker, date)
        decision = result["decision"]

        if decision in alert_on:
            self.send_alert(ticker, decision, result)

        return result

    def send_alert(self, ticker, decision, details):
        """Send alert via email/SMS"""

        message = f"""
        🚨 TRADING SIGNAL ALERT 🚨

        Ticker: {ticker}
        Decision: {decision}
        Time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

        Summary:
        {details['final_decision'][:500]}
        """

        # Send email
        if self.email_config:
            self.send_email(message)

        # Send SMS
        if self.sms_config:
            self.send_sms(f"{ticker}: {decision}")

    def send_email(self, message):
        """Send email alert (implementation depends on provider)"""
        import smtplib
        from email.mime.text import MIMEText

        msg = MIMEText(message)
        msg['Subject'] = 'Trading Signal Alert'
        msg['From'] = self.email_config['from']
        msg['To'] = self.email_config['to']

        with smtplib.SMTP(self.email_config['smtp_server']) as server:
            server.send_message(msg)

    def send_sms(self, message):
        """Send SMS alert (using Twilio or similar)"""
        from twilio.rest import Client

        client = Client(
            self.sms_config['account_sid'],
            self.sms_config['auth_token']
        )

        client.messages.create(
            body=message,
            from_=self.sms_config['from_number'],
            to=self.sms_config['to_number']
        )

# Usage
alert_system = TradingAlertSystem(
    email_config={
        'smtp_server': 'smtp.gmail.com',
        'from': 'alerts@example.com',
        'to': 'trader@example.com'
    }
)

# Monitor watchlist
watchlist = ["NVDA", "TSLA", "AAPL"]
for ticker in watchlist:
    alert_system.analyze_and_alert(ticker, "2024-05-10", alert_on=["BUY"])
```

---

## Best Practices

### 1. Error Handling

```python
from tenacity import retry, stop_after_attempt, wait_exponential

@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=4, max=10))
def robust_analysis(ticker, date):
    """Analyze with retry logic"""
    try:
        ta = TradingAgentsGraph(debug=False, config=DEFAULT_CONFIG.copy())
        final_state, decision = ta.propagate(ticker, date)
        return {"success": True, "decision": decision}
    except Exception as e:
        print(f"Attempt failed: {e}")
        raise

# Usage
result = robust_analysis("NVDA", "2024-05-10")
```

### 2. Logging

```python
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('trading_agents.log'),
        logging.StreamHandler()
    ]
)

logger = logging.getLogger('TradingAgents')

def logged_analysis(ticker, date):
    """Analysis with comprehensive logging"""
    logger.info(f"Starting analysis for {ticker} on {date}")

    try:
        result = analyze_stock(ticker, date)
        logger.info(f"{ticker} analysis complete: {result['decision']}")
        return result
    except Exception as e:
        logger.error(f"Analysis failed for {ticker}: {e}", exc_info=True)
        raise
```

### 3. Caching Results

```python
import pickle
from pathlib import Path

class AnalysisCache:
    def __init__(self, cache_dir="cache"):
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(exist_ok=True)

    def get_cache_key(self, ticker, date):
        return f"{ticker}_{date}.pkl"

    def get(self, ticker, date):
        """Get cached analysis if exists"""
        cache_file = self.cache_dir / self.get_cache_key(ticker, date)
        if cache_file.exists():
            with open(cache_file, 'rb') as f:
                return pickle.load(f)
        return None

    def set(self, ticker, date, result):
        """Cache analysis result"""
        cache_file = self.cache_dir / self.get_cache_key(ticker, date)
        with open(cache_file, 'wb') as f:
            pickle.dump(result, f)

# Usage
cache = AnalysisCache()

def cached_analyze(ticker, date):
    """Analyze with caching"""
    # Check cache first
    cached_result = cache.get(ticker, date)
    if cached_result:
        print(f"Using cached result for {ticker}")
        return cached_result

    # Perform analysis
    result = analyze_stock(ticker, date)

    # Cache result
    cache.set(ticker, date, result)

    return result
```

---

This usage guide provides comprehensive patterns for integrating TradingAgents into production workflows, from simple stock analysis to complex automated trading systems.
