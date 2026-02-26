# TradingAgents - Configuration Guide

## Configuration Overview

TradingAgents provides extensive configuration options to customize:
- LLM providers and models
- Data vendors
- Workflow parameters
- Agent selection
- Debug and logging settings

All configuration is managed through a Python dictionary that can be customized and passed to `TradingAgentsGraph`.

---

## Default Configuration

**Location**: `tradingagents/default_config.py`

```python
DEFAULT_CONFIG = {
    # ===== LLM Configuration =====
    "llm_provider": "openai",
    "deep_think_llm": "o4-mini",
    "quick_think_llm": "gpt-4o-mini",
    "backend_url": "https://api.openai.com/v1",

    # ===== Workflow Configuration =====
    "max_debate_rounds": 1,
    "max_risk_discuss_rounds": 1,

    # ===== Data Vendor Configuration =====
    "data_vendors": {
        "core_stock_apis": "yfinance",
        "technical_indicators": "yfinance",
        "fundamental_data": "alpha_vantage",
        "news_data": "alpha_vantage",
    },

    # ===== Tool-Level Vendor Overrides (Optional) =====
    "tool_vendors": {
        # Example: Override specific tools
        # "get_balance_sheet": "alpha_vantage",
        # "get_news": "openai",
    }
}
```

---

## LLM Configuration

### 1. LLM Provider Selection

**Supported Providers**:
- `openai`: OpenAI GPT models
- `anthropic`: Anthropic Claude models
- `google`: Google Gemini models
- `ollama`: Local models via Ollama
- `openrouter`: OpenRouter aggregator

**Configuration**:
```python
config = DEFAULT_CONFIG.copy()

# For OpenAI (default)
config["llm_provider"] = "openai"
config["backend_url"] = "https://api.openai.com/v1"

# For Anthropic
config["llm_provider"] = "anthropic"
config["backend_url"] = "https://api.anthropic.com/v1"

# For Google
config["llm_provider"] = "google"
config["backend_url"] = "https://generativelanguage.googleapis.com/v1"

# For Ollama (local)
config["llm_provider"] = "ollama"
config["backend_url"] = "http://localhost:11434"

# For OpenRouter
config["llm_provider"] = "openrouter"
config["backend_url"] = "https://openrouter.ai/api/v1"
```

### 2. Model Selection

TradingAgents uses **two types of LLMs**:

#### Deep Thinking LLM
Used for complex reasoning tasks:
- Investment debate (bull/bear researchers)
- Risk debate (risk analysts)
- Final decisions (managers)

**Recommended Models**:
- **OpenAI**: `o4-mini`, `o1-preview`, `gpt-4o`
- **Anthropic**: `claude-3-opus-20240229`, `claude-3-sonnet-20240229`
- **Google**: `gemini-1.5-pro`

#### Quick Thinking LLM
Used for data analysis and report generation:
- Analysts (market, fundamentals, news, social)
- Trader synthesis
- Signal processing

**Recommended Models**:
- **OpenAI**: `gpt-4o-mini`, `gpt-4o`, `gpt-3.5-turbo`
- **Anthropic**: `claude-3-haiku-20240307`, `claude-3-sonnet-20240229`
- **Google**: `gemini-1.5-flash`

**Configuration Examples**:

```python
# Production setup (high quality)
config["deep_think_llm"] = "o1-preview"
config["quick_think_llm"] = "gpt-4o"

# Balanced setup (quality + cost)
config["deep_think_llm"] = "gpt-4o"
config["quick_think_llm"] = "gpt-4o-mini"

# Budget setup (cost-optimized)
config["deep_think_llm"] = "gpt-4o-mini"
config["quick_think_llm"] = "gpt-4o-mini"

# Anthropic setup
config["llm_provider"] = "anthropic"
config["deep_think_llm"] = "claude-3-opus-20240229"
config["quick_think_llm"] = "claude-3-haiku-20240307"

# Local setup (Ollama)
config["llm_provider"] = "ollama"
config["deep_think_llm"] = "llama3.1:70b"
config["quick_think_llm"] = "llama3.1:8b"
```

### 3. Cost Optimization Strategies

**Cost Breakdown by Model** (Approximate):

| Model | Input (per 1M tokens) | Output (per 1M tokens) | Use Case |
|-------|----------------------|------------------------|----------|
| o1-preview | $15.00 | $60.00 | Production deep thinking |
| gpt-4o | $2.50 | $10.00 | Balanced production |
| gpt-4o-mini | $0.15 | $0.60 | Budget-friendly |
| claude-3-opus | $15.00 | $75.00 | High-quality reasoning |
| claude-3-sonnet | $3.00 | $15.00 | Balanced |
| claude-3-haiku | $0.25 | $1.25 | Fast and cheap |

**Cost-Saving Tips**:

1. **Use cheaper model for analysts**:
   ```python
   config["quick_think_llm"] = "gpt-4o-mini"  # Analysts don't need o1
   ```

2. **Reduce debate rounds**:
   ```python
   config["max_debate_rounds"] = 1  # Instead of 2-3
   config["max_risk_discuss_rounds"] = 1
   ```

3. **Limit selected analysts**:
   ```python
   ta = TradingAgentsGraph(
       selected_analysts=["market", "fundamentals"],  # Only 2 instead of 4
       config=config
   )
   ```

4. **Use local models for testing**:
   ```python
   config["llm_provider"] = "ollama"
   config["deep_think_llm"] = "llama3.1:8b"
   ```

**Estimated Costs per Analysis**:

| Setup | Cost per Stock |
|-------|----------------|
| Production (o1-preview + gpt-4o) | $0.50 - $1.00 |
| Balanced (gpt-4o + gpt-4o-mini) | $0.10 - $0.25 |
| Budget (gpt-4o-mini only) | $0.02 - $0.05 |
| Local (Ollama) | $0.00 |

---

## Workflow Configuration

### 1. Debate Rounds

Control how many rounds of debate occur:

```python
# Investment debate (bull vs. bear)
config["max_debate_rounds"] = 1  # Default: 1
# Each round: bull argues → bear argues

# Risk debate (risky vs. safe vs. neutral)
config["max_risk_discuss_rounds"] = 1  # Default: 1
# Each round: risky → safe → neutral
```

**Impact of Increasing Rounds**:
- **Pros**: More thorough analysis, deeper reasoning
- **Cons**: Higher costs, longer execution time, diminishing returns

**Recommendations**:
- **High-stakes decisions**: 2-3 rounds
- **Normal trading**: 1 round (default)
- **Quick screening**: 0 rounds (skip debate, go straight to managers)

**Example: Skip Debates Entirely**:
```python
config["max_debate_rounds"] = 0  # Skip bull/bear debate
config["max_risk_discuss_rounds"] = 0  # Skip risk debate
# Analysts → Research Manager → Trader → Risk Manager (direct)
```

### 2. Analyst Selection

Choose which analysts to include:

```python
# All analysts (default)
ta = TradingAgentsGraph(
    selected_analysts=["market", "fundamentals", "news", "social"],
    config=config
)

# Technical + Fundamentals only
ta = TradingAgentsGraph(
    selected_analysts=["market", "fundamentals"],
    config=config
)

# Single analyst (fastest)
ta = TradingAgentsGraph(
    selected_analysts=["market"],
    config=config
)
```

**Analyst Options**:
- `"market"`: Technical/chart analysis
- `"fundamentals"`: Financial statement analysis
- `"news"`: News and macro events
- `"social"`: Social media sentiment

**Use Cases**:
- **Day trading**: `["market", "social"]` (technical + sentiment)
- **Value investing**: `["fundamentals", "news"]` (fundamentals + macro)
- **Swing trading**: `["market", "fundamentals"]` (technical + fundamentals)
- **Quick scan**: `["market"]` (technical only)

---

## Data Vendor Configuration

### 1. Category-Level Configuration

Set vendors for entire tool categories:

```python
config["data_vendors"] = {
    "core_stock_apis": "yfinance",        # Stock price data
    "technical_indicators": "yfinance",   # MACD, RSI, etc.
    "fundamental_data": "alpha_vantage",  # Balance sheet, income statement
    "news_data": "alpha_vantage",         # News and sentiment
}
```

**Available Vendors by Category**:

| Category | Supported Vendors | Free/Paid |
|----------|------------------|-----------|
| `core_stock_apis` | yfinance, alpha_vantage | Free |
| `technical_indicators` | yfinance, alpha_vantage | Free |
| `fundamental_data` | yfinance, alpha_vantage, openai | Alpha Vantage free (limited), OpenAI paid |
| `news_data` | alpha_vantage, openai, google | Alpha Vantage free (limited), OpenAI/Google paid |

### 2. Tool-Level Overrides

Override specific tools to use different vendors:

```python
config["tool_vendors"] = {
    # Get balance sheet from Alpha Vantage instead of yfinance
    "get_balance_sheet": "alpha_vantage",

    # Get news from OpenAI instead of Alpha Vantage
    "get_news": "openai",

    # Get fundamental data from OpenAI
    "get_fundamental_data": "openai",
}
```

**Available Tools**:

**Core Stock APIs**:
- `get_stock_data`: OHLCV price data

**Technical Indicators**:
- `get_indicators`: MACD, RSI, SMA, Bollinger Bands, ATR, etc.

**Fundamental Data**:
- `get_balance_sheet`: Assets, liabilities, equity
- `get_income_statement`: Revenue, expenses, profit
- `get_cash_flow_statement`: Operating, investing, financing cash flows
- `get_fundamental_data`: P/E, market cap, EPS, dividend yield

**News Data**:
- `get_news`: Company-specific news
- `get_global_news`: Broader market news
- `get_insider_sentiment`: Insider trading data
- `get_insider_transactions`: Insider buy/sell details

### 3. Vendor Fallback Mechanism

If a primary vendor fails, the system automatically tries fallback vendors:

```python
# routing_interface.py logic:
def route_to_vendor(tool_name, category, **kwargs):
    # 1. Try tool-level override if specified
    if tool_name in config["tool_vendors"]:
        try:
            return execute_tool(config["tool_vendors"][tool_name], **kwargs)
        except:
            pass  # Fall through to category-level

    # 2. Try category-level primary vendor
    primary_vendor = config["data_vendors"][category]
    try:
        return execute_tool(primary_vendor, **kwargs)
    except:
        pass  # Fall through to fallbacks

    # 3. Try fallback vendors for category
    fallback_vendors = get_fallback_vendors(category)
    for vendor in fallback_vendors:
        try:
            return execute_tool(vendor, **kwargs)
        except:
            continue

    # 4. Return error message
    return f"Error: All vendors failed for {tool_name}"
```

**Example Scenario**:
```python
# Configuration
config["data_vendors"]["fundamental_data"] = "alpha_vantage"

# If Alpha Vantage is down or rate-limited:
# 1. Try alpha_vantage (fails)
# 2. Try yfinance fallback (succeeds)
# 3. Return yfinance data

# No intervention needed - automatic resilience
```

### 4. API Key Requirements

**Required Environment Variables**:

```bash
# OpenAI (required for LLM and embeddings)
export OPENAI_API_KEY="sk-..."

# Alpha Vantage (optional, for fundamentals/news)
export ALPHA_VANTAGE_API_KEY="your_key_here"

# Anthropic (if using Anthropic LLMs)
export ANTHROPIC_API_KEY="sk-ant-..."

# Google (if using Google LLMs or news)
export GOOGLE_API_KEY="AIza..."
```

**Free Tier Limits**:
- **yfinance**: No API key needed, unlimited (rate-limited by Yahoo)
- **Alpha Vantage**: 25 requests/day (free), 60 requests/minute (TradingAgents-specific)
- **OpenAI**: Pay-as-you-go (no free tier)
- **Anthropic**: Pay-as-you-go (no free tier)

**Vendor Selection Strategy**:

```python
# Free setup (no API keys except OpenAI for LLM)
config["data_vendors"] = {
    "core_stock_apis": "yfinance",
    "technical_indicators": "yfinance",
    "fundamental_data": "yfinance",
    "news_data": "yfinance",  # Limited news via yfinance
}

# Best quality setup (paid)
config["data_vendors"] = {
    "core_stock_apis": "yfinance",
    "technical_indicators": "yfinance",
    "fundamental_data": "alpha_vantage",
    "news_data": "openai",  # Use GPT for news analysis
}
```

---

## Environment Setup

### 1. Installation

```bash
# Clone repository
git clone https://github.com/TauricResearch/TradingAgents.git
cd TradingAgents

# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
```

### 2. Environment Variables

Create a `.env` file:

```bash
# Required
OPENAI_API_KEY=sk-...

# Optional
ALPHA_VANTAGE_API_KEY=your_key_here
ANTHROPIC_API_KEY=sk-ant-...
GOOGLE_API_KEY=AIza...
```

Load in Python:

```python
from dotenv import load_dotenv
load_dotenv()

# Now environment variables are available
```

### 3. Verify Setup

```python
import os
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

# Check API keys
assert os.getenv("OPENAI_API_KEY"), "Missing OPENAI_API_KEY"

# Initialize framework
ta = TradingAgentsGraph(debug=True, config=DEFAULT_CONFIG.copy())

# Test with a simple analysis
final_state, decision = ta.propagate("AAPL", "2024-01-15")
print(f"Decision: {decision}")
```

---

## Advanced Configuration

### 1. Custom Configuration Profiles

Create configuration profiles for different use cases:

```python
# profiles.py

BASE_CONFIG = {
    "llm_provider": "openai",
    "backend_url": "https://api.openai.com/v1",
}

# Day trading profile: Fast execution, technical focus
DAY_TRADING_CONFIG = {
    **BASE_CONFIG,
    "deep_think_llm": "gpt-4o-mini",
    "quick_think_llm": "gpt-4o-mini",
    "max_debate_rounds": 0,  # Skip debates for speed
    "max_risk_discuss_rounds": 0,
    "data_vendors": {
        "core_stock_apis": "yfinance",
        "technical_indicators": "yfinance",
        "fundamental_data": "yfinance",
        "news_data": "yfinance",
    }
}

# Value investing profile: Deep analysis, fundamental focus
VALUE_INVESTING_CONFIG = {
    **BASE_CONFIG,
    "deep_think_llm": "o1-preview",
    "quick_think_llm": "gpt-4o",
    "max_debate_rounds": 3,  # Deep debate
    "max_risk_discuss_rounds": 2,
    "data_vendors": {
        "core_stock_apis": "yfinance",
        "technical_indicators": "yfinance",
        "fundamental_data": "alpha_vantage",
        "news_data": "openai",
    }
}

# Swing trading profile: Balanced
SWING_TRADING_CONFIG = {
    **BASE_CONFIG,
    "deep_think_llm": "gpt-4o",
    "quick_think_llm": "gpt-4o-mini",
    "max_debate_rounds": 1,
    "max_risk_discuss_rounds": 1,
    "data_vendors": {
        "core_stock_apis": "yfinance",
        "technical_indicators": "yfinance",
        "fundamental_data": "alpha_vantage",
        "news_data": "alpha_vantage",
    }
}

# Usage
from profiles import SWING_TRADING_CONFIG
ta = TradingAgentsGraph(
    selected_analysts=["market", "fundamentals"],
    debug=True,
    config=SWING_TRADING_CONFIG
)
```

### 2. Multi-Model Setup

Use different models for different agent types:

```python
# This requires modifying the TradingAgentsGraph initialization
# Current implementation uses 2 LLM types (deep/quick)
# For more granular control, you can modify the source code:

# In trading_graph.py:
def __init__(self, selected_analysts, debug, config):
    # Instead of just deep_think_llm and quick_think_llm
    # Create specialized LLMs for each agent type
    self.analyst_llm = self._initialize_llm(config["analyst_llm"])
    self.debate_llm = self._initialize_llm(config["debate_llm"])
    self.manager_llm = self._initialize_llm(config["manager_llm"])
    self.trader_llm = self._initialize_llm(config["trader_llm"])
```

### 3. Custom Memory Configuration

Customize the embedding model and vector database:

```python
# In trading_graph.py, modify memory initialization:
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import Chroma

# Use different embedding model
custom_embeddings = OpenAIEmbeddings(
    model="text-embedding-3-large",  # Higher quality
    dimensions=1536
)

# Create memories with custom embeddings
bull_memory = FinancialSituationMemory(
    collection_name="bull_researcher_situations",
    embedding_model=custom_embeddings
)
```

### 4. Debug and Logging Configuration

```python
# Enable debug mode for streaming output
ta = TradingAgentsGraph(
    selected_analysts=["market", "fundamentals"],
    debug=True,  # Streams execution progress to console
    config=config
)

# Disable debug for production
ta = TradingAgentsGraph(
    selected_analysts=["market", "fundamentals"],
    debug=False,  # Silent execution
    config=config
)
```

**Debug Output Includes**:
- Agent execution progress
- Tool calls and results
- Debate arguments
- Decision reasoning
- Timing information

### 5. Recursion Limits

LangGraph has built-in recursion limits to prevent infinite loops:

```python
# In propagation.py:
def propagate(self, company_name, trade_date):
    initial_state = self._create_initial_state(company_name, trade_date)

    # Invoke graph with recursion limit
    final_state = self.graph.invoke(
        initial_state,
        {
            "recursion_limit": 100  # Maximum 100 agent steps
        }
    )

    return final_state
```

**Adjust if needed**:
```python
# For complex workflows with many debate rounds
final_state = self.graph.invoke(
    initial_state,
    {
        "recursion_limit": 200  # Increase limit
    }
)
```

---

## Configuration Best Practices

### 1. Development vs. Production

**Development**:
```python
DEV_CONFIG = DEFAULT_CONFIG.copy()
DEV_CONFIG["deep_think_llm"] = "gpt-4o-mini"
DEV_CONFIG["quick_think_llm"] = "gpt-4o-mini"
DEV_CONFIG["max_debate_rounds"] = 1
DEV_CONFIG["max_risk_discuss_rounds"] = 0
# Fast, cheap, for testing
```

**Production**:
```python
PROD_CONFIG = DEFAULT_CONFIG.copy()
PROD_CONFIG["deep_think_llm"] = "o1-preview"
PROD_CONFIG["quick_think_llm"] = "gpt-4o"
PROD_CONFIG["max_debate_rounds"] = 2
PROD_CONFIG["max_risk_discuss_rounds"] = 1
# High quality, thorough analysis
```

### 2. Error Handling

```python
try:
    ta = TradingAgentsGraph(
        selected_analysts=["market", "fundamentals"],
        debug=True,
        config=config
    )
    final_state, decision = ta.propagate("NVDA", "2024-05-10")
except Exception as e:
    print(f"Error: {e}")
    # Handle vendor failures, API rate limits, etc.
```

### 3. Configuration Validation

```python
def validate_config(config):
    """Validate configuration before use"""
    required_keys = ["llm_provider", "deep_think_llm", "quick_think_llm"]
    for key in required_keys:
        assert key in config, f"Missing required config key: {key}"

    # Validate LLM provider
    valid_providers = ["openai", "anthropic", "google", "ollama", "openrouter"]
    assert config["llm_provider"] in valid_providers, \
        f"Invalid LLM provider: {config['llm_provider']}"

    # Validate data vendors
    assert "data_vendors" in config, "Missing data_vendors configuration"

    return True

# Usage
validate_config(my_config)
ta = TradingAgentsGraph(config=my_config)
```

### 4. Cost Tracking

```python
import tiktoken

def estimate_cost(model, input_tokens, output_tokens):
    """Estimate cost for a model run"""
    costs = {
        "gpt-4o": {"input": 2.50, "output": 10.00},
        "gpt-4o-mini": {"input": 0.15, "output": 0.60},
        "o1-preview": {"input": 15.00, "output": 60.00},
    }

    cost_per_input = costs[model]["input"] * (input_tokens / 1_000_000)
    cost_per_output = costs[model]["output"] * (output_tokens / 1_000_000)

    return cost_per_input + cost_per_output

# Track usage
total_cost = 0.0
# ... after each agent execution, track tokens and accumulate cost
```

---

## Configuration Reference Table

| Parameter | Type | Default | Options | Description |
|-----------|------|---------|---------|-------------|
| `llm_provider` | str | `"openai"` | openai, anthropic, google, ollama, openrouter | LLM provider to use |
| `deep_think_llm` | str | `"o4-mini"` | Any model name | Model for complex reasoning (debates, decisions) |
| `quick_think_llm` | str | `"gpt-4o-mini"` | Any model name | Model for data analysis (analysts, reports) |
| `backend_url` | str | Provider-specific | Valid API URL | API endpoint URL |
| `max_debate_rounds` | int | `1` | 0-10 | Number of bull/bear debate rounds |
| `max_risk_discuss_rounds` | int | `1` | 0-10 | Number of risk debate rounds |
| `data_vendors` | dict | See default | Vendor mapping | Category-level vendor configuration |
| `tool_vendors` | dict | `{}` | Vendor mapping | Tool-level vendor overrides |
| `selected_analysts` | list | All 4 | ["market", "fundamentals", "news", "social"] | Which analysts to include |
| `debug` | bool | `False` | True/False | Enable streaming debug output |

---

## Troubleshooting

### Common Configuration Issues

**1. Missing API Key**
```
Error: OpenAI API key not found
Solution: Set OPENAI_API_KEY environment variable
```

**2. Invalid Model Name**
```
Error: Model "gpt-5" not found
Solution: Use valid model name (gpt-4o, gpt-4o-mini, etc.)
```

**3. Vendor Rate Limit**
```
Error: Alpha Vantage rate limit exceeded
Solution: Switch to yfinance or wait for rate limit reset
```

**4. Tool Not Found**
```
Error: Tool "get_xyz" not implemented for vendor "abc"
Solution: Check vendor supports the tool or use fallback
```

**5. Recursion Limit Exceeded**
```
Error: Maximum recursion depth exceeded
Solution: Increase recursion_limit or reduce debate rounds
```

---

This configuration guide provides complete control over the TradingAgents framework, enabling customization for any trading strategy, budget, or performance requirement.
