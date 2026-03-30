# Moneycontrol News Feed Options

## ✅ Option 1: RSS Feeds (Official, No Scraping Needed)

Moneycontrol provides RSS 2.0 XML feeds under the `/rss/` path. These are live and functional:

| Feed | URL |
|---|---|
| Latest News | `https://www.moneycontrol.com/rss/latestnews.xml` |
| Buzzing Stocks | `https://www.moneycontrol.com/rss/buzzingstocks.xml` |
| Market Reports | `https://www.moneycontrol.com/rss/marketreports.xml` |
| Economy | `https://www.moneycontrol.com/rss/economy.xml` |

Other likely feeds following the same pattern:
- `https://www.moneycontrol.com/rss/MFNews.xml`
- `https://www.moneycontrol.com/rss/business.xml`
- `https://www.moneycontrol.com/rss/results.xml`

Each feed returns standard RSS items with `<title>`, `<link>`, `<description>`, `<pubDate>`, and `<guid>`. You can parse these with any RSS library (e.g. `feedparser` in Python, `rss-parser` in Node.js).

> **⚠️ Caveat:** The feeds appear to have stopped being updated around mid-2024 (last build dates show June 2024). They may have been quietly deprecated or served from a cache. Worth monitoring to confirm freshness.

---

## ⚠️ Option 2: News Sitemap (For Bulk/Historical Access)

The `robots.txt` reveals official news sitemaps:

- `https://www.moneycontrol.com/news/news-sitemap.xml`
- `https://www.moneycontrol.com/news/index-sitemap-2025.xml`
- `https://www.moneycontrol.com/news/index-sitemap-2026.xml`

These are Google News-style sitemaps listing article URLs with timestamps — useful for discovering recent articles programmatically without scraping HTML.

---

## ❌ No Official Public API

Moneycontrol does **not** offer a public developer API for news. There is no API documentation or developer portal available on the site.

---

## 🚫 Scraping Considerations

The `robots.txt` explicitly blocks several AI/data bots:

- `GPTBot`
- `CCBot`
- `Google-Extended`
- `DataForSeoBot`

While standard scrapers are not blocked by name, scraping HTML pages would be against their Terms of Service and is technically fragile.

---

## ✅ Recommendation

Use the **RSS feeds** — they are official, structured, and require no scraping. Just verify freshness before relying on them in production.