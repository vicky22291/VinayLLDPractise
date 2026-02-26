# TradingAgents - Agent System Documentation

## Agent Hierarchy Overview

The TradingAgents framework employs **10 specialized agent types** organized into four functional layers:

1. **Analyst Layer** (4 agents): Data gathering and preliminary analysis
2. **Research Layer** (3 agents): Investment opportunity debate
3. **Trading Layer** (1 agent): Decision synthesis
4. **Risk Management Layer** (4 agents): Risk evaluation and final approval

## Agent Layer Breakdown

```
Layer 1: ANALYSTS (Parallel Execution)
├── Market Analyst (Technical Analysis)
├── Fundamentals Analyst (Financial Statements)
├── News Analyst (Macroeconomic Events)
└── Social Media Analyst (Sentiment Analysis)

Layer 2: RESEARCHERS (Sequential Debate)
├── Bull Researcher (Bullish Arguments)
├── Bear Researcher (Bearish Arguments)
└── Research Manager (Debate Judge)

Layer 3: TRADING
└── Trader (Decision Synthesis)

Layer 4: RISK MANAGEMENT (Sequential Debate)
├── Risky/Aggressive Analyst (High-Reward Perspective)
├── Safe/Conservative Analyst (Risk-Mitigation Perspective)
├── Neutral Analyst (Balanced Perspective)
└── Risk Manager (Final Approval/Rejection)
```

---

## LAYER 1: ANALYSTS

Analysts are **tool-using agents** that gather and analyze data from external sources. They run in parallel to maximize efficiency.

### 1. Market Analyst (Technical Analysis)

**File**: `tradingagents/agents/analysts/market_analyst.py`

**Purpose**: Perform technical analysis using price data and indicators

**Tools Used**:
- `get_stock_data`: Retrieve OHLCV (Open, High, Low, Close, Volume) data
- `get_indicators`: Calculate technical indicators

**Technical Indicators Analyzed**:
- **SMA** (Simple Moving Averages): 20-day, 50-day, 200-day
- **MACD** (Moving Average Convergence Divergence): Trend momentum
- **RSI** (Relative Strength Index): Overbought/oversold conditions
- **Bollinger Bands**: Volatility and price extremes
- **ATR** (Average True Range): Volatility measurement
- **Stochastic Oscillator**: Momentum indicator

**Prompt Focus**:
```
You are a Senior Technical Analyst specializing in chart patterns
and technical indicators. Analyze:
- Trend identification (uptrend, downtrend, sideways)
- Support and resistance levels
- Indicator signals (bullish/bearish crossovers)
- Volume analysis
- Momentum indicators

Provide a concise technical summary with actionable insights.
```

**Output**: `state["market_report"]`

**Example Report**:
```
TECHNICAL ANALYSIS SUMMARY - NVDA (2024-05-10)

Trend: Strong uptrend with price above all major SMAs
- Price: $450.32
- SMA(20): $425.10 | SMA(50): $398.55 | SMA(200): $365.20

MACD: Bullish signal (MACD: 12.5, Signal: 8.3) - positive divergence
RSI: 68.5 - approaching overbought but still room to run
Bollinger Bands: Price near upper band, suggesting strong momentum
Volume: Above average, confirming trend strength

SIGNALS: BUY - Strong technical setup with momentum confirmation
```

---

### 2. Fundamentals Analyst

**File**: `tradingagents/agents/analysts/fundamentals_analyst.py`

**Purpose**: Analyze financial health using company financial statements

**Tools Used**:
- `get_balance_sheet`: Assets, liabilities, equity
- `get_income_statement`: Revenue, expenses, profit
- `get_cash_flow_statement`: Operating, investing, financing cash flows
- `get_fundamental_data`: P/E ratio, market cap, dividend yield, EPS, etc.

**Financial Metrics Analyzed**:
- **Valuation**: P/E ratio, P/B ratio, PEG ratio
- **Profitability**: Gross margin, operating margin, net margin, ROE, ROA
- **Liquidity**: Current ratio, quick ratio
- **Leverage**: Debt-to-equity, interest coverage
- **Growth**: Revenue growth, earnings growth, free cash flow growth

**Prompt Focus**:
```
You are a Certified Financial Analyst (CFA) specializing in
fundamental analysis. Evaluate:
- Financial statement health (balance sheet strength)
- Profitability trends (income statement)
- Cash flow quality (operating cash flow vs. net income)
- Valuation metrics (relative to industry and historical)
- Growth trajectory

Provide a fundamental health score and investment perspective.
```

**Output**: `state["fundamentals_report"]`

**Example Report**:
```
FUNDAMENTAL ANALYSIS - NVDA (Q1 2024)

Financial Health: STRONG
- Total Assets: $65.7B | Total Liabilities: $22.1B
- Debt-to-Equity: 0.34 (Conservative leverage)
- Current Ratio: 4.2 (Excellent liquidity)

Profitability: EXCELLENT
- Gross Margin: 64.5% (Industry-leading)
- Operating Margin: 47.2%
- Net Margin: 36.7%
- ROE: 88.5% (Exceptional capital efficiency)

Valuation:
- P/E Ratio: 45.2 (Premium but justified by growth)
- PEG Ratio: 1.8 (Reasonable for growth rate)
- P/S Ratio: 28.3

Growth Trajectory:
- Revenue Growth (YoY): +265%
- EPS Growth (YoY): +461%
- Free Cash Flow: $14.2B (Robust)

VERDICT: BUY - Exceptional fundamentals justify premium valuation
```

---

### 3. News Analyst

**File**: `tradingagents/agents/analysts/news_analyst.py`

**Purpose**: Analyze macroeconomic trends and news events

**Tools Used**:
- `get_news`: Company-specific news
- `get_global_news`: Broader market and sector news
- `get_insider_sentiment`: Insider trading activities (optional)

**Analysis Focus**:
- Macroeconomic trends (GDP, inflation, interest rates)
- Sector-specific developments
- Company announcements (earnings, products, partnerships)
- Regulatory changes
- Geopolitical events affecting markets

**Prompt Focus**:
```
You are a Senior Market News Analyst specializing in macro trends
and their market impact. Analyze:
- Key news events and their implications
- Macroeconomic indicators and trends
- Sector dynamics
- Catalysts and headwinds
- Market sentiment

Provide a news-driven perspective on investment timing.
```

**Output**: `state["news_report"]`

**Example Report**:
```
NEWS & MACRO ANALYSIS - NVDA (2024-05-10)

Recent Headlines:
1. NVIDIA announces new AI chip architecture (May 8)
   Impact: BULLISH - Technological leadership reinforced

2. Federal Reserve signals rate stability (May 7)
   Impact: BULLISH - Growth stocks benefit from stable rates

3. Microsoft expands NVIDIA GPU orders for Azure AI (May 6)
   Impact: BULLISH - Demand confirmation from hyperscalers

Macroeconomic Context:
- Interest Rates: Stable at 5.25% (favorable for growth)
- Inflation: Trending down (3.4% CPI)
- GDP Growth: 2.1% (Healthy economy)

Sector Analysis:
- Semiconductor sector up 18% YTD
- AI infrastructure spending accelerating
- Supply chain constraints easing

OUTLOOK: POSITIVE - Strong fundamental drivers with favorable macro backdrop
```

---

### 4. Social Media Analyst

**File**: `tradingagents/agents/analysts/social_media_analyst.py`

**Purpose**: Gauge retail investor sentiment and social media buzz

**Tools Used**:
- `get_reddit_sentiment`: Reddit discussions (WallStreetBets, r/stocks, etc.)
- `get_twitter_sentiment`: Twitter/X mentions and sentiment
- `get_stocktwits_sentiment`: StockTwits sentiment scores

**Note**: In current implementation, these tools may route to news APIs or sentiment proxies depending on vendor configuration.

**Analysis Focus**:
- Retail sentiment (bullish/bearish ratio)
- Social media volume and trending topics
- Influencer opinions
- Meme stock dynamics
- Fear/greed indicators

**Prompt Focus**:
```
You are a Social Media Sentiment Analyst tracking retail investor
behavior. Analyze:
- Overall sentiment (bullish vs. bearish)
- Volume of discussions (trending or fading)
- Key themes in retail conversations
- Notable influencer positions
- Contrarian indicators

Provide a sentiment summary with crowd psychology insights.
```

**Output**: `state["sentiment_report"]`

**Example Report**:
```
SOCIAL SENTIMENT ANALYSIS - NVDA (2024-05-10)

Overall Sentiment: EXTREMELY BULLISH (82% bullish mentions)

Platform Breakdown:
- Reddit (r/WallStreetBets): 4,523 mentions (87% bullish)
- Twitter/X: Trending #3 in finance (75% positive)
- StockTwits: Sentiment Score: 78/100 (Very Bullish)

Key Themes:
1. "AI Revolution" - Dominant narrative
2. "Data center demand unstoppable"
3. "Next stop $500" - Price target discussions
4. Fear of missing out (FOMO) prevalent

Influencer Activity:
- 3 major fintwit accounts upgraded to BUY
- Hedge fund manager mentions NVDA as top holding

WARNING SIGNALS:
- Sentiment may be overheated (contrarian indicator)
- Retail positioning very crowded

ASSESSMENT: BULLISH sentiment but watch for reversal if profit-taking occurs
```

---

## LAYER 2: RESEARCHERS

Researchers engage in **structured debate** to evaluate investment opportunities from opposing perspectives. They use **memory-augmented reasoning** to learn from past debates.

### 5. Bull Researcher

**File**: `tradingagents/agents/researchers/bull_researcher.py`

**Purpose**: Advocate for bullish/long positions with growth focus

**Characteristics**:
- **Optimistic bias**: Emphasizes growth potential and positive catalysts
- **Growth-focused**: Highlights revenue acceleration, market expansion
- **Memory-driven**: Recalls past successful bullish calls

**Prompt Focus**:
```
You are a Growth-Oriented Investment Researcher with a bullish
perspective. Review all analyst reports and argue FOR investment:

- Identify growth catalysts
- Highlight competitive advantages
- Emphasize positive trends
- Counter bearish concerns
- Provide upside price targets

Be persuasive but honest. Use evidence from analyst reports.
```

**Debate Strategy**:
1. **Opening Argument**: Present strongest bullish case
2. **Rebuttal**: Counter bear's concerns with data
3. **Closing**: Summarize why potential rewards outweigh risks

**Memory Integration**:
```python
memories = bull_memory.get_memories(current_situation, k=2)
# Retrieves similar past situations where bull stance paid off
```

**Example Argument**:
```
BULL CASE FOR NVDA

THESIS: NVIDIA is at the epicenter of the AI revolution with
unmatched competitive moats.

Growth Catalysts:
1. AI chip demand growing 10x faster than supply
2. New H100/H200 GPUs command premium pricing with no competition
3. Software ecosystem (CUDA) creates lock-in effects
4. TAM expansion: Gaming → Data Center → Automotive → Edge AI

Competitive Advantages:
- 90%+ market share in AI training chips
- 10-year technology lead over competitors
- Vertical integration from chip to software
- Customer stickiness due to CUDA ecosystem

Valuation Justification:
- P/E of 45x justified by 265% revenue growth
- PEG ratio of 1.8 is reasonable for this growth
- Comparable companies (AMD, Intel) trade at higher P/E with lower growth

Risk Mitigation:
- Strong balance sheet (minimal debt)
- Diversified customer base (hyperscalers + enterprises)
- Proven execution track record

Price Target: $525 (6-month) based on DCF with 35% revenue CAGR

VERDICT: STRONG BUY - Generational wealth creation opportunity
```

---

### 6. Bear Researcher

**File**: `tradingagents/agents/researchers/bear_researcher.py`

**Purpose**: Identify risks and argue against investment

**Characteristics**:
- **Skeptical stance**: Emphasizes risks, overvaluation, headwinds
- **Risk-focused**: Highlights competition, market saturation, regulatory risks
- **Memory-driven**: Recalls past situations where caution was warranted

**Prompt Focus**:
```
You are a Risk-Focused Investment Researcher with a bearish
perspective. Review all analyst reports and argue AGAINST investment:

- Identify overvaluation signals
- Highlight competitive threats
- Emphasize downside risks
- Challenge bullish assumptions
- Provide downside price targets

Be critical but fair. Use evidence from analyst reports.
```

**Debate Strategy**:
1. **Opening Argument**: Present strongest bearish case
2. **Rebuttal**: Challenge bull's optimistic assumptions
3. **Closing**: Emphasize why risks outweigh potential rewards

**Memory Integration**:
```python
memories = bear_memory.get_memories(current_situation, k=2)
# Retrieves similar past situations where bearish caution prevented losses
```

**Example Argument**:
```
BEAR CASE FOR NVDA

THESIS: NVIDIA is severely overvalued with multiple headwinds
that could trigger significant correction.

Overvaluation Concerns:
- P/E of 45x is 3x higher than S&P 500 average
- P/S ratio of 28x is extreme by historical standards
- Priced for perfection - any miss will cause sharp selloff
- Market cap of $1.2T requires sustained hypergrowth

Competitive Threats:
1. AMD launching competitive MI300 series (40% cheaper)
2. Cloud hyperscalers developing custom chips (AWS Trainium, Google TPU)
3. Intel's Gaudi chips gaining traction
4. Chinese competitors (Huawei, Alibaba) in domestic market

Demand Sustainability Risks:
- AI hype cycle may be peaking (Gartner hype curve)
- Enterprise AI ROI unproven - potential spending pullback
- Data center capex may moderate in H2 2024
- GPU oversupply risk if demand normalizes

Technical Concerns:
- RSI at 68.5 approaching overbought (>70)
- Retail sentiment at 82% bullish (contrarian signal)
- Institutional profit-taking likely near ATHs

Macro Headwinds:
- Export restrictions to China (25% of revenue at risk)
- Potential semiconductor cyclicality
- Rising interest rates historically compress P/E multiples

Price Target: $325 (6-month) if market reprices to sustainable P/E of 30x

VERDICT: SELL or WAIT - Risk/reward unfavorable at current levels
```

---

### 7. Research Manager (Investment Debate Judge)

**File**: `tradingagents/agents/managers/research_manager.py`

**Purpose**: Judge the bull/bear debate and create balanced investment plan

**Characteristics**:
- **Impartial evaluator**: Weighs both perspectives objectively
- **Evidence-based**: Prioritizes data over rhetoric
- **Synthesis role**: Creates actionable investment plan from debate

**Prompt Focus**:
```
You are a Senior Portfolio Manager judging an investment debate.

Review:
- Bull Researcher's arguments
- Bear Researcher's counter-arguments
- Original analyst reports

Evaluate:
- Strength of evidence on both sides
- Quality of reasoning
- Risk/reward balance

Provide:
- Judgment on which side presented stronger case
- Balanced investment recommendation
- Suggested position sizing
- Key conditions to monitor
```

**Decision Framework**:
1. **Evidence Quality**: Which side used more concrete data?
2. **Risk Assessment**: Are bull's rewards > bear's risks?
3. **Timing**: Is now the right time given market conditions?
4. **Conviction Level**: High, Medium, or Low confidence?

**Memory Integration**:
```python
memories = invest_judge_memory.get_memories(current_situation, k=2)
# Retrieves similar past debate judgments and their outcomes
```

**Output**: `state["investment_debate_state"]["judge_decision"]`

**Example Judgment**:
```
INVESTMENT DEBATE JUDGMENT - NVDA

DECISION: FAVOR BULL CASE (65% conviction)

Analysis:
Both researchers presented compelling arguments. However, the bull case
has stronger evidentiary support:

Bull Strengths:
✓ Concrete revenue growth data (265% YoY) supports valuation
✓ Competitive moat (CUDA ecosystem) is real and defensible
✓ AI infrastructure demand validated by multiple hyperscaler orders
✓ Strong fundamental health (low debt, high margins)

Bear Weaknesses:
⚠ Competitive threats are future risks, not current reality
⚠ Valuation criticism valid but growth justifies premium
⚠ Technical overbought signals are short-term noise

Bear Valid Points:
✓ Valuation does leave little room for error
✓ Sentiment is extremely bullish (potential contrarian signal)
✓ Export restrictions pose real geopolitical risk

INVESTMENT PLAN:

Recommendation: BUY with position sizing caution
Position Size: 3-5% of portfolio (not overweight)
Entry Strategy: Scale in over 2-3 weeks to average entry
Stop Loss: $400 (-11% from current price)

Upside Target: $525 (6-month, +17%)
Downside Risk: $325 (-28% if bear case materializes)
Risk/Reward Ratio: 1:1.5 (Acceptable)

Monitoring Triggers:
- Quarterly earnings (must beat by >5% to justify valuation)
- Hyperscaler AI spending trends
- AMD competitive product launches
- Export restriction changes

Conviction: MEDIUM-HIGH - Buy but size appropriately for risk
```

---

## LAYER 3: TRADING

### 8. Trader

**File**: `tradingagents/agents/trader/trader.py`

**Purpose**: Synthesize all analysis into final BUY/SELL/HOLD decision

**Characteristics**:
- **Synthesis role**: Integrates technical, fundamental, news, sentiment, and research
- **Decision-maker**: Produces clear trading signal
- **Risk-aware**: Considers position sizing and risk management

**Inputs**:
- Market report (technical analysis)
- Fundamentals report
- News report
- Sentiment report
- Investment debate judge's plan

**Prompt Focus**:
```
You are a Senior Portfolio Trader responsible for executing
investment decisions.

Review ALL analysis:
- Technical: [market_report]
- Fundamentals: [fundamentals_report]
- News: [news_report]
- Sentiment: [sentiment_report]
- Research Recommendation: [judge_decision]

Decide: BUY, SELL, or HOLD

Provide:
- Clear decision with conviction level
- Entry/exit strategy
- Position sizing recommendation
- Key risks to monitor
- Trade rationale

Be decisive but prudent.
```

**Decision Framework**:
```python
if all_signals_align() and risk_reward_favorable():
    return "BUY" + detailed_plan
elif major_red_flags() or poor_risk_reward():
    return "SELL" or "AVOID"
else:
    return "HOLD" + wait_for_better_entry
```

**Memory Integration**:
```python
memories = trader_memory.get_memories(current_situation, k=2)
# Retrieves similar past trading decisions and outcomes
```

**Output**: State update with trader's decision (passed to risk management)

**Example Decision**:
```
TRADING DECISION - NVDA (2024-05-10)

DECISION: BUY (70% conviction)

Rationale:
All four analyst reports align with the research manager's bullish
recommendation. The confluence of signals justifies a long position:

✓ Technical: Strong uptrend, bullish indicators (MACD, SMA alignment)
✓ Fundamentals: Exceptional growth (265% revenue) with strong margins
✓ News: Positive catalysts (new product launch, hyperscaler orders)
✓ Sentiment: Bullish (caution: may be overheated)
✓ Research Debate: Bull case stronger, judge recommends BUY

Trade Execution Plan:

Entry Strategy: Scaled approach
- Week 1: 40% of position at current price ($450)
- Week 2: 30% on any dip to $440
- Week 3: 30% final tranche

Position Sizing: 4% of portfolio (moderate conviction)

Risk Management:
- Stop Loss: $400 (hard stop, -11%)
- Take Profit 1: $500 (+11%, trim 30% of position)
- Take Profit 2: $525 (+17%, trim another 40%)
- Let 30% run with trailing stop

Holding Period: 3-6 months (swing trade)

Key Risks to Monitor:
1. Earnings miss (next report in 6 weeks)
2. AMD competitive product release
3. Hyperscaler capex guidance reductions
4. Macro deterioration (recession fears)

Exit Triggers:
- Fundamental: Revenue growth decelerates below 150%
- Technical: Break below $400 support
- Sentiment: Major insider selling
- News: Export ban expansion

Expected Outcome:
- Base Case: +15-20% in 6 months
- Bull Case: +30%+ if AI momentum sustains
- Bear Case: -10% stop loss triggered

FINAL CALL: BUY NVDA with disciplined risk management
```

---

## LAYER 4: RISK MANAGEMENT

The risk management layer conducts a **three-way debate** evaluating the trader's decision from different risk perspectives, culminating in final approval/rejection by the Risk Manager.

### 9. Risky/Aggressive Analyst

**File**: `tradingagents/agents/risk_mgmt/aggressive_debator.py`

**Purpose**: Advocate for high-reward strategies and aggressive positioning

**Characteristics**:
- **Growth-maximizing**: Prioritizes upside over downside
- **Aggressive stance**: Encourages larger position sizes
- **FOMO-aware**: Don't miss generational opportunities

**Prompt Focus**:
```
You are an Aggressive Risk Analyst focused on maximizing returns.
Evaluate the trader's decision:

- Is the position size too conservative?
- Are we missing upside by being too cautious?
- Should we use leverage to amplify returns?
- What's the opportunity cost of not going big?

Argue for taking more risk when warranted by high-conviction setups.
```

**Example Analysis**:
```
AGGRESSIVE RISK PERSPECTIVE - NVDA BUY

ASSESSMENT: Trader's 4% position is TOO CONSERVATIVE

This is a generational AI infrastructure play - we should be OVERWEIGHT:

Upside Case:
- AI TAM growing from $50B to $500B over 5 years
- NVDA has 90% market share with no credible competition
- Historical AI cycles show 10x returns possible
- Current setup mirrors Amazon in 2001, Apple in 2008

Recommended Adjustments:
❌ Current: 4% position, scaled entry
✓ Proposed: 8% position, immediate full entry

Why More Aggressive:
1. Opportunity Cost: Miss 20%+ upside while "scaling in"
2. Conviction Mismatch: 70% conviction warrants 6-8% allocation
3. Risk/Reward: 1.5:1 is excellent - go bigger
4. Diversification: Overweight top conviction ideas

Leverage Consideration:
- Use 1.2x leverage (options or margin) to amplify returns
- Sell cash-secured puts at $440 to get paid while scaling in

Risk Tolerance:
- $400 stop loss is fine, but we can afford -11% on 8% position
- Maximum loss: -0.88% of portfolio (acceptable)

VERDICT: INCREASE POSITION TO 8% - Don't miss this train
```

---

### 10. Safe/Conservative Analyst

**File**: `tradingagents/agents/risk_mgmt/conservative_debator.py`

**Purpose**: Emphasize risk mitigation and capital preservation

**Characteristics**:
- **Capital preservation**: Prioritizes downside protection
- **Conservative stance**: Recommends smaller positions and tighter stops
- **Bear market aware**: Always considers what can go wrong

**Prompt Focus**:
```
You are a Conservative Risk Analyst focused on capital preservation.
Evaluate the trader's decision:

- What are the downside risks?
- Is the position size too large for this risk?
- Are stop losses adequate?
- What black swan events could occur?

Argue for caution and risk reduction to protect capital.
```

**Example Analysis**:
```
CONSERVATIVE RISK PERSPECTIVE - NVDA BUY

ASSESSMENT: Trader's plan has SIGNIFICANT RISKS

While the bull case is compelling, the risk/reward is UNFAVORABLE at
current valuations:

Downside Risks:
1. Valuation Extreme: P/E of 45x leaves no room for error
2. Sentiment Overheated: 82% bullish is historically a reversal signal
3. Technical Overbought: RSI 68.5 suggests pullback imminent
4. Macro Uncertainty: Rates could stay higher longer

Risk Catalog:
- Earnings Miss: -15% overnight drop (historical precedent)
- Competitive Product: AMD launch could trigger -10% selloff
- Export Ban: China restrictions could hit revenue 25%
- Market Correction: Tech selloff would amplify downside

Recommended Adjustments:
❌ Current: 4% position, $400 stop (-11%)
✓ Proposed: 2% position, $425 stop (-6%)

Why More Conservative:
1. Position Sizing: 4% too large for single stock concentration
2. Stop Loss: $400 is -11% (unacceptable for growth stock)
3. Entry Timing: Wait for pullback to $420-430 for better entry
4. Opportunity Cost: Cash is safe - no need to chase

Alternative Strategies:
- WAIT for dip: Patience will provide better risk/reward
- Sell puts: Collect premium while waiting for entry
- Smaller starter: 1% position, add only on confirmation

Maximum Acceptable Loss:
- 2% position × -6% stop = -0.12% portfolio impact
- vs. Current: 4% position × -11% = -0.44% (TOO MUCH)

VERDICT: REDUCE POSITION TO 2% or WAIT for better entry
```

---

### 11. Neutral Analyst

**File**: `tradingagents/agents/risk_mgmt/neutral_debator.py`

**Purpose**: Provide balanced perspective between aggressive and conservative views

**Characteristics**:
- **Balanced approach**: Middle ground between risk and reward
- **Data-driven**: Focuses on empirical risk metrics
- **Pragmatic**: Practical risk management without extremes

**Prompt Focus**:
```
You are a Neutral Risk Analyst providing balanced risk assessment.
Evaluate the trader's decision objectively:

- Calculate risk-adjusted returns (Sharpe ratio)
- Compare to portfolio risk budget
- Assess diversification impact
- Provide data-driven risk metrics

Find the middle ground between aggressive and conservative positions.
```

**Example Analysis**:
```
NEUTRAL RISK PERSPECTIVE - NVDA BUY

ASSESSMENT: Trader's 4% position is APPROPRIATE with minor tweaks

Quantitative Risk Analysis:

Position Sizing Model:
- Kelly Criterion: Optimal size = (p × b - q) / b
  - p (win probability): 0.65 (based on conviction)
  - b (win magnitude): 1.5 (risk/reward ratio)
  - q (loss probability): 0.35
  - Optimal = 4.2% ✓ (trader's 4% is correct)

Risk Metrics:
- VaR (95%): Maximum 1-day loss = 0.28% of portfolio
- Expected Shortfall: Worst 5% scenarios = 0.35% loss
- Portfolio Beta Impact: +0.03 (minimal)

Diversification Analysis:
- Current tech allocation: 18%
- After NVDA: 22% (acceptable, below 25% limit)
- Correlation to portfolio: 0.72 (moderate)

Risk-Adjusted Return:
- Expected Return: +15% in 6 months
- Volatility: 35% (annual)
- Sharpe Ratio: 1.2 (good for single stock)

Balanced Recommendations:

Position Size: KEEP 4% ✓
- Aligns with Kelly Criterion
- Fits portfolio risk budget
- Neither too aggressive nor too conservative

Stop Loss: Adjust to $410 (not $400)
- Tighter than aggressive ($400)
- Looser than conservative ($425)
- -9% drawdown is acceptable for 4% position

Entry Timing: Split the difference
- 50% immediate entry (capture momentum)
- 50% scale in over 2 weeks (average down if dip)

Take Profit: Add partial targets
- 30% at +10% ($495)
- 40% at +20% ($540)
- 30% trailing stop for runners

Risk Mitigation:
- Portfolio rebalance if NVDA grows to >6%
- Hedge with sector ETF puts if tech >25%
- Monitor daily VaR and cut if exceeds 0.5%

VERDICT: APPROVE 4% position with $410 stop and split entry
```

---

### 12. Risk Manager (Final Judgment)

**File**: `tradingagents/agents/managers/risk_manager.py`

**Purpose**: Make final approval/rejection of trading decision after evaluating all risk perspectives

**Characteristics**:
- **Ultimate authority**: Final decision on trade execution
- **Portfolio-level view**: Considers overall portfolio risk
- **Balanced synthesizer**: Integrates all three risk perspectives

**Inputs**:
- Trader's original decision
- Aggressive analyst's perspective
- Conservative analyst's perspective
- Neutral analyst's perspective

**Prompt Focus**:
```
You are the Chief Risk Officer making the final trade approval decision.

Review:
- Trader's BUY/SELL/HOLD decision
- Aggressive Risk Analyst's arguments
- Conservative Risk Analyst's concerns
- Neutral Risk Analyst's metrics

Decide:
- APPROVE (execute as planned)
- APPROVE WITH MODIFICATIONS (specify changes)
- REJECT (do not execute)

Provide final decision with clear rationale and any required adjustments.
```

**Decision Framework**:
```python
if risk_metrics_acceptable() and portfolio_limits_ok():
    if aggressive_and_conservative_both_approve():
        return "APPROVE"
    elif neutral_provides_good_middle_ground():
        return "APPROVE WITH MODIFICATIONS"
    else:
        return "FURTHER REVIEW NEEDED"
else:
    return "REJECT"
```

**Memory Integration**:
```python
memories = risk_manager_memory.get_memories(current_situation, k=2)
# Retrieves similar past risk decisions and outcomes
```

**Output**: `state["final_decision"]` (the ultimate trading signal)

**Example Final Decision**:
```
RISK MANAGEMENT FINAL DECISION - NVDA

DECISION: APPROVE WITH MODIFICATIONS

Final Risk Assessment:

After reviewing all three risk perspectives, I'm approving the NVDA
BUY with specific modifications to balance risk and reward:

Perspective Evaluation:
- Aggressive Analyst: Valid points on opportunity cost, but 8% is too concentrated
- Conservative Analyst: Too cautious given strong fundamentals, but entry timing concern valid
- Neutral Analyst: Best data-driven approach, recommendations adopted

APPROVED TRADE PARAMETERS:

Action: BUY NVDA
Position Size: 4% of portfolio ✓ (neutral's recommendation)
Entry Strategy: Split entry (modified)
  - 50% immediate at market ($450)
  - 25% limit order at $440
  - 25% limit order at $430

Risk Management:
Stop Loss: $410 (-9%) ✓ (neutral's tighter stop)
Position Limits:
  - Max position: 6% (if price appreciates)
  - Rebalance trigger: Trim if >6%
  - Tech sector cap: 25% (currently 18%, will be 22%)

Take Profit Targets:
  - TP1: $495 (+10%) → Sell 30%
  - TP2: $540 (+20%) → Sell 40%
  - Trailing stop: 30% remainder with 8% trail

Portfolio Risk Limits:
✓ VaR (95%): 0.28% < 0.50% limit
✓ Sector concentration: 22% < 25% limit
✓ Single stock: 4% < 8% limit
✓ Total portfolio beta: 1.08 < 1.20 limit

Monitoring Requirements:
1. Daily P&L check: Flag if >3% intraday move
2. Weekly rebalance: If position drifts >5% from target
3. Earnings watch: Review before next ER (6 weeks)
4. Stop loss: Hard stop, no discretion

Approval Conditions:
- Execute only if market opens with VIX <20
- Cancel if NVDA gaps down >3% on any news
- Review if tech sector ETF (QQQ) breaks 50-day SMA

Risk-Adjusted Expected Outcome:
- Base Case: +12% over 6 months (risk-adjusted)
- Maximum Loss: -9% (stop loss)
- Sharpe Ratio: 1.2
- Win Probability: 65%

Rationale for Approval:
The trader's original analysis is sound, and the neutral analyst's
quantitative risk metrics support execution. While the conservative
analyst's caution is noted, the strong fundamental and technical
alignment justify the position. The aggressive analyst's push for 8%
is rejected as excessive concentration risk.

FINAL AUTHORIZATION: APPROVED - Execute modified plan

Signed: Risk Manager
Date: 2024-05-10
```

---

## Memory System Integration

All agents with memory (Bull, Bear, Trader, Research Manager, Risk Manager) follow the same pattern:

### Memory Retrieval Pattern
```python
# Before making decision/argument
current_situation = format_current_context(state)
memories = agent_memory.get_memories(current_situation, k=2)

# Memories formatted in prompt
prompt = f"""
[Agent role and instructions]

Similar past situations:
{format_memories(memories)}

Current situation:
{current_situation}

Your analysis:
"""
```

### Memory Storage Pattern
```python
# After actual trade outcome is known
outcome_data = {
    "situation": original_situation,
    "agent_contribution": bull_argument / bear_argument / trader_decision,
    "outcome": "WIN" if return > 0 else "LOSS",
    "return": actual_return_percentage
}

agent_memory.add_situations([outcome_data])
```

### Memory Benefits
1. **Learn from wins**: Reinforce successful patterns
2. **Learn from losses**: Avoid repeating mistakes
3. **Context-aware**: Similar situations retrieve relevant memories
4. **Continuous improvement**: Performance improves over time

---

## Agent Communication Patterns

### Pattern 1: Tool-Using Agents (Analysts)
```
Agent → LLM with tools → Tool call decision
                ↓
        Execute tool (data API)
                ↓
        Tool result → LLM
                ↓
        Final report → State update
```

### Pattern 2: Debate Agents (Bull/Bear, Risk Debators)
```
Agent → Retrieve memories → Format prompt with opponent's history
                ↓
        LLM generates argument
                ↓
        Argument → Update own history → State update
                ↓
        Opponent's turn
```

### Pattern 3: Judge Agents (Managers)
```
Judge → Review all arguments → Retrieve similar past judgments
                ↓
        LLM evaluates evidence quality
                ↓
        Generate judgment → State update
```

### Pattern 4: Synthesis Agents (Trader)
```
Trader → Review ALL reports → Retrieve past decisions
                ↓
        LLM synthesizes information
                ↓
        Generate decision → State update
```

---

## Agent Development Patterns

### Creating a New Agent

1. **Define Purpose**: What specific role does this agent fill?
2. **Determine Type**: Tool-using, debate, judge, or synthesis?
3. **Create Higher-Order Function**:
   ```python
   def create_new_agent(llm, tools, memory, config):
       def agent_node(state: AgentState) -> dict:
           # Extract context from state
           # Retrieve memories if applicable
           # Format prompt
           # Invoke LLM
           # Return state update
           return {"field_to_update": result}
       return agent_node
   ```
4. **Add to Graph**: Register in `GraphSetup._create_graph()`
5. **Update Conditional Logic**: If needed, modify routing
6. **Test in Isolation**: Unit test with mock state

---

This agent system creates a sophisticated multi-perspective decision-making framework that mirrors institutional trading operations while leveraging LLM capabilities for nuanced reasoning and continuous learning.
