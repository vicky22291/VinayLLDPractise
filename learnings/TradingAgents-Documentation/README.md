# TradingAgents - Complete Documentation

Comprehensive documentation for understanding and working with the TradingAgents multi-agent LLM trading framework.

## ✨ Enhanced with Visual Diagrams

This documentation includes **14+ Mermaid diagrams** for visual understanding:
- 📊 System architecture visualizations
- 🔄 Workflow sequence diagrams
- 🎯 Agent interaction flows
- 📈 Data routing diagrams
- 🗺️ State transition maps

## Documentation Index

This documentation is organized into six focused guides, each covering a specific aspect of the TradingAgents system:

### 📘 [01 - Overview](./01-OVERVIEW.md)
**Start here if you're new to TradingAgents**

- What is TradingAgents?
- Core features and capabilities
- Technology stack
- High-level architecture
- Use cases
- Quick start example
- Key design principles

**Read this to**: Get a high-level understanding of what TradingAgents does and how it works.

---

### 🏗️ [02 - Architecture](./02-ARCHITECTURE.md)
**Deep dive into system design and workflow**

- System architecture overview
- Core components breakdown
- Workflow execution flow
- State management
- Memory system
- Agent creation patterns
- Data abstraction layer
- Configuration architecture
- Performance characteristics
- Extension points

**Read this to**: Understand how the system is built, how components interact, and how data flows through the framework.

---

### 🤖 [03 - Agent System](./03-AGENT-SYSTEM.md)
**Detailed documentation of all 10+ agent types**

- Agent hierarchy overview
- **Analyst Layer** (4 agents)
  - Market Analyst (Technical Analysis)
  - Fundamentals Analyst (Financial Statements)
  - News Analyst (Macroeconomic Events)
  - Social Media Analyst (Sentiment)
- **Research Layer** (3 agents)
  - Bull Researcher
  - Bear Researcher
  - Research Manager
- **Trading Layer** (1 agent)
  - Trader
- **Risk Management Layer** (4 agents)
  - Risky/Aggressive Analyst
  - Safe/Conservative Analyst
  - Neutral Analyst
  - Risk Manager
- Memory integration
- Agent communication patterns
- Agent development guide

**Read this to**: Learn what each agent does, how they reason, and how they collaborate.

---

### ⚙️ [04 - Configuration Guide](./04-CONFIGURATION-GUIDE.md)
**Complete configuration reference**

- Default configuration
- LLM configuration (provider, models)
- Workflow configuration (debate rounds, analyst selection)
- Data vendor configuration
- Environment setup
- Advanced configuration
- Configuration profiles (day trading, value investing, etc.)
- Cost optimization strategies
- Best practices
- Troubleshooting

**Read this to**: Learn how to customize TradingAgents for your specific needs, optimize costs, and configure different trading strategies.

---

### 💻 [05 - Usage Guide](./05-USAGE-GUIDE.md)
**Practical integration patterns and code examples**

- Quick start
- Core usage patterns
  - Single stock analysis
  - Multi-stock screening
  - Daily analysis with scheduling
  - Backtesting framework
  - Learning from outcomes (reflection)
- Integration patterns
  - REST API integration
  - Database integration
  - Webhook integration
  - Paper trading integration
- Advanced usage patterns
  - Custom analyst selection
  - Batch processing
  - Alert systems
- Best practices (error handling, logging, caching)

**Read this to**: See practical examples of how to use TradingAgents in real-world scenarios and integrate it into your workflows.

---

### 📊 [06 - Data Flow and Vendors](./06-DATA-FLOW-AND-VENDORS.md)
**Understanding data sources and vendor management**

- Data flow architecture
- Tool categories and available tools
  - Core Stock APIs
  - Technical Indicators
  - Fundamental Data
  - News Data
- Vendor-specific implementations
  - yfinance (free, reliable)
  - Alpha Vantage (comprehensive, rate-limited)
  - OpenAI (GPT-powered analysis)
  - Google (news aggregation)
  - Local (cached data)
- Routing logic and fallback mechanism
- Data format standardization
- Vendor selection strategies
- Rate limits and quotas
- Extending the data layer
- Troubleshooting
- Data caching best practices

**Read this to**: Understand how TradingAgents retrieves financial data, how to configure different data sources, and how to add new vendors.

---

## Quick Navigation

### By User Type

**👨‍💼 Business/Strategy Users**
1. Start with [Overview](./01-OVERVIEW.md) to understand capabilities
2. Read [Agent System](./03-AGENT-SYSTEM.md) to see how decisions are made
3. Check [Configuration Guide](./04-CONFIGURATION-GUIDE.md) for strategy profiles

**👨‍💻 Developers/Integrators**
1. Start with [Overview](./01-OVERVIEW.md) for context
2. Read [Architecture](./02-ARCHITECTURE.md) for technical understanding
3. Follow [Usage Guide](./05-USAGE-GUIDE.md) for integration patterns
4. Reference [Configuration Guide](./04-CONFIGURATION-GUIDE.md) as needed

**🔬 Researchers/Academics**
1. Start with [Overview](./01-OVERVIEW.md) and [Architecture](./02-ARCHITECTURE.md)
2. Deep dive into [Agent System](./03-AGENT-SYSTEM.md) for multi-agent dynamics
3. Study [Data Flow](./06-DATA-FLOW-AND-VENDORS.md) for data handling

**💰 Traders/Analysts**
1. Read [Overview](./01-OVERVIEW.md) to understand the framework
2. Check [Agent System](./03-AGENT-SYSTEM.md) to understand analysis layers
3. Use [Usage Guide](./05-USAGE-GUIDE.md) for practical examples
4. Configure with [Configuration Guide](./04-CONFIGURATION-GUIDE.md)

### By Task

| Task | Relevant Documentation |
|------|----------------------|
| **Getting started** | [Overview](./01-OVERVIEW.md) |
| **Understanding how it works** | [Architecture](./02-ARCHITECTURE.md) |
| **Customizing agents** | [Agent System](./03-AGENT-SYSTEM.md) |
| **Changing LLMs or settings** | [Configuration Guide](./04-CONFIGURATION-GUIDE.md) |
| **Integrating into application** | [Usage Guide](./05-USAGE-GUIDE.md) |
| **Adding data sources** | [Data Flow](./06-DATA-FLOW-AND-VENDORS.md) |
| **Optimizing costs** | [Configuration Guide](./04-CONFIGURATION-GUIDE.md) §3 |
| **Backtesting strategies** | [Usage Guide](./05-USAGE-GUIDE.md) §1.4 |
| **Understanding decisions** | [Agent System](./03-AGENT-SYSTEM.md) |
| **Troubleshooting** | [Configuration Guide](./04-CONFIGURATION-GUIDE.md) §11, [Data Flow](./06-DATA-FLOW-AND-VENDORS.md) §10 |

---

## Document Statistics

| Document | Pages (approx) | Topics Covered | Target Audience |
|----------|---------------|----------------|-----------------|
| 01-Overview | 8 | High-level intro, features, quick start | Everyone |
| 02-Architecture | 20 | System design, components, workflow | Developers, Architects |
| 03-Agent System | 25 | Agent details, reasoning, collaboration | All technical users |
| 04-Configuration | 18 | Setup, customization, optimization | Developers, Traders |
| 05-Usage Guide | 22 | Code examples, integration patterns | Developers |
| 06-Data Flow | 20 | Data sources, vendors, routing | Developers, Data Engineers |
| **Total** | **~113** | **Comprehensive coverage** | **All users** |

---

## Key Concepts Summary

### Multi-Agent Architecture
TradingAgents uses 10+ specialized AI agents that collaborate through structured workflows, mirroring how institutional trading firms operate.

### Debate-Driven Decisions
Bull vs. Bear researchers debate investment opportunities, and Risky vs. Safe vs. Neutral analysts evaluate risk, ensuring multiple perspectives.

### Memory-Augmented Learning
Agents learn from past trading decisions using ChromaDB vector storage, improving future performance.

### Vendor-Agnostic Data
Flexible multi-vendor data layer with automatic fallback supports yfinance, Alpha Vantage, OpenAI, Google, and local sources.

### Configurable Workflows
Extensive customization via LLM selection, analyst selection, debate rounds, and vendor configuration.

---

## Example Workflows

### 1. Quick Stock Analysis
```python
from tradingagents.graph.trading_graph import TradingAgentsGraph
from tradingagents.default_config import DEFAULT_CONFIG

ta = TradingAgentsGraph(debug=True, config=DEFAULT_CONFIG.copy())
final_state, decision = ta.propagate("NVDA", "2024-05-10")
print(f"Decision: {decision}")
```
See: [Usage Guide §1](./05-USAGE-GUIDE.md#1-single-stock-analysis)

### 2. Multi-Stock Screening
```python
watchlist = ["AAPL", "MSFT", "GOOGL", "AMZN", "NVDA"]
results = screen_stocks(watchlist)
print(f"BUY signals: {len(results['buy'])}")
```
See: [Usage Guide §1.2](./05-USAGE-GUIDE.md#2-multi-stock-screening)

### 3. Custom Configuration
```python
config = DEFAULT_CONFIG.copy()
config["deep_think_llm"] = "gpt-4o"
config["max_debate_rounds"] = 2

ta = TradingAgentsGraph(
    selected_analysts=["market", "fundamentals"],
    config=config
)
```
See: [Configuration Guide §2](./04-CONFIGURATION-GUIDE.md#2-model-selection)

### 4. Learning from Outcomes
```python
result = analyze_stock("NVDA", "2024-05-10")
# ... wait for actual outcome ...
ta.reflect_and_remember({
    "ticker": "NVDA",
    "actual_return": 0.15,
    "decision": "BUY"
})
```
See: [Usage Guide §1.5](./05-USAGE-GUIDE.md#5-learning-from-outcomes-reflection)

---

## Common Questions

### Q: Where do I start?
**A**: Read [01-Overview](./01-OVERVIEW.md) first, then try the quick start example.

### Q: How do I reduce costs?
**A**: See [Configuration Guide §2.3](./04-CONFIGURATION-GUIDE.md#3-cost-optimization-strategies) for cost optimization strategies.

### Q: Can I use different LLM providers?
**A**: Yes! See [Configuration Guide §2.1](./04-CONFIGURATION-GUIDE.md#1-llm-provider-selection) for OpenAI, Anthropic, Google, and Ollama support.

### Q: How do I add custom data sources?
**A**: See [Data Flow §11](./06-DATA-FLOW-AND-VENDORS.md#extending-the-data-layer) for vendor extension guide.

### Q: What agents are involved in decisions?
**A**: See [Agent System](./03-AGENT-SYSTEM.md) for complete breakdown of all 10+ agent types.

### Q: How do I integrate into my trading bot?
**A**: See [Usage Guide §2](./05-USAGE-GUIDE.md#integration-patterns) for REST API, database, and webhook patterns.

### Q: Can I backtest strategies?
**A**: Yes! See [Usage Guide §1.4](./05-USAGE-GUIDE.md#4-backtesting-framework) for backtesting implementation.

### Q: How does memory work?
**A**: See [Architecture §5](./02-ARCHITECTURE.md#5-memory-system) and [Agent System §12](./03-AGENT-SYSTEM.md#memory-system-integration) for memory details.

---

## Additional Resources

### Official Repository
https://github.com/TauricResearch/TradingAgents

### Related Technologies
- **LangGraph**: https://langchain-ai.github.io/langgraph/
- **LangChain**: https://python.langchain.com/
- **ChromaDB**: https://www.trychroma.com/
- **yfinance**: https://github.com/ranaroussi/yfinance
- **Alpha Vantage**: https://www.alphavantage.co/

### Community
- GitHub Issues: https://github.com/TauricResearch/TradingAgents/issues
- Tauric Research: https://tauric.com

---

## Documentation Feedback

This documentation was created to provide comprehensive understanding of the TradingAgents framework. Each document is designed to be:

- **Standalone**: Can be read independently
- **Cross-referenced**: Links to related sections
- **Practical**: Includes code examples
- **Comprehensive**: Covers all aspects of the system

If you have questions or suggestions for improving this documentation, please open an issue on the GitHub repository.

---

## License

TradingAgents is licensed under the Apache-2.0 License.

This documentation is provided as-is to help users understand and work with the TradingAgents framework.

---

**Last Updated**: February 2026
**Documentation Version**: 1.0
**TradingAgents Version**: Latest (as of repository state)

---

## Quick Reference Card

```
┌─────────────────────────────────────────────────────────────┐
│                  TradingAgents Quick Reference              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Initialize:                                                │
│    ta = TradingAgentsGraph(config=DEFAULT_CONFIG.copy())    │
│                                                             │
│  Analyze:                                                   │
│    final_state, decision = ta.propagate("TICKER", "DATE")   │
│                                                             │
│  Decision Types:                                            │
│    • "BUY"  - Long opportunity identified                   │
│    • "SELL" - Short or exit opportunity                     │
│    • "HOLD" - Wait or maintain position                     │
│                                                             │
│  Agent Flow:                                                │
│    Analysts → Debate → Trader → Risk Mgmt → Final Decision  │
│                                                             │
│  Configuration:                                             │
│    config["deep_think_llm"] = "model-name"                  │
│    config["max_debate_rounds"] = N                          │
│    config["data_vendors"]["category"] = "vendor"            │
│                                                             │
│  Customization:                                             │
│    selected_analysts = ["market", "fundamentals"]           │
│    debug = True  # Stream execution progress                │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

Happy Trading! 🚀📈
