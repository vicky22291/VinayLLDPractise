# MarkTechPost Blog Scraper — Page Structure & Research Guide
> **Purpose:** This document provides all the structural details needed to build a Playwright (Python) scraper for blog posts on [marktechpost.com](https://www.marktechpost.com), specifically targeting the **Open Source** category.
---
## Table of Contents
1. [Site Overview](#1-site-overview)
2. [robots.txt](#2-robotstxt)
3. [Scraping Approaches](#3-scraping-approaches)
4. [Approach A — WordPress REST API](#4-approach-a--wordpress-rest-api)
5. [Approach B — HTML Scraping](#5-approach-b--html-scraping)
6. [Listing Page Structure](#6-listing-page-structure)
7. [Post Card HTML Structure](#7-post-card-html-structure)
8. [Post Card Playwright Selectors](#8-post-card-playwright-selectors)
9. [Individual Post Page Structure](#9-individual-post-page-structure)
10. [Individual Post Playwright Selectors](#10-individual-post-playwright-selectors)
11. [OpenGraph & JSON-LD Metadata](#11-opengraph--json-ld-metadata)
12. [Playwright Setup & Best Practices](#12-playwright-setup--best-practices)
13. [Recommended Scraping Strategy](#13-recommended-scraping-strategy)
14. [Output Data Schema](#14-output-data-schema)
15. [Key URLs Reference](#15-key-urls-reference)
---
## 1. Site Overview
| Property | Value |
|---|---|
| **Site URL** | `https://www.marktechpost.com` |
| **CMS** | WordPress (Newspaper Theme / Tagdiv Composer) |
| **Target Category** | Open Source |
| **Target Category URL** | `https://www.marktechpost.com/category/technology/open-source/` |
| **Open Source Category ID** (WP) | `11832` |
| **Parent Category** | Technology (`id: 1447`) |
| **Total posts in Open Source** | ~501 |
| **Total posts on entire site** | ~10,814 |
| **HTML listing pages (Open Source)** | 51 |
| **Posts per HTML listing page** | 100 |
| **WP REST API available** | ✅ Yes |
| **Login required** | ❌ No |
| **CAPTCHA observed** | ❌ No |
---
## 2. robots.txt
```
User-agent: *
Disallow: /wp-admin/
Allow: /wp-admin/admin-ajax.php
```
> Crawling the public front-end and the WordPress REST API is **not restricted** by robots.txt.
---
## 3. Scraping Approaches
Two approaches are available. The REST API is strongly recommended as the primary method.
| | Approach A: REST API | Approach B: HTML Scraping |
|---|---|---|
| **Speed** | Very fast (6 requests for all 501 posts) | Slower (51 listing pages + up to 501 post pages) |
| **Reliability** | High — structured JSON | Medium — depends on HTML structure |
| **Full content** | Yes (in `content.rendered`) | Yes (from `.td-post-content`) |
| **Pagination** | Via `page` query param + response headers | Via URL pattern `/page/{N}/` |
| **Recommended** | ✅ Primary | ✅ Fallback / full HTML content |
---
## 4. Approach A — WordPress REST API
### Base Endpoint
```
https://www.marktechpost.com/wp-json/wp/v2/posts
```
### Query Parameters
| Parameter | Example Value | Description |
|---|---|---|
| `categories` | `11832` | Filter by Open Source category ID |
| `per_page` | `100` | Items per page (max: 100) |
| `page` | `1` | Page number (1-indexed) |
| `_embed` | `author,wp:featuredmedia,wp:term` | Embed related data inline |
| `_fields` | See below | Limit returned fields to reduce payload |
### Recommended `_fields` Value
```
id,date,date_gmt,modified,slug,link,title,content,excerpt,author,
featured_media,categories,tags,jetpack_featured_media_url,yoast_head_json
```
### Example Request URL
```
GET https://www.marktechpost.com/wp-json/wp/v2/posts
    ?categories=11832
    &per_page=100
    &page=1
    &_embed=author,wp:featuredmedia,wp:term
    &_fields=id,date,slug,link,title,content,excerpt,author,featured_media,
             categories,tags,jetpack_featured_media_url
```
### Pagination via Response Headers
| Header | Example | Description |
|---|---|---|
| `X-WP-Total` | `501` | Total number of matching posts |
| `X-WP-TotalPages` | `6` | Total pages at current `per_page` |
**Formula:** `total_pages = ceil(X-WP-Total / per_page)`  
With `per_page=100` and 501 posts → **6 pages**.
### API Response Fields (per post object)
| Field Path | Type | Description |
|---|---|---|
| `id` | int | WordPress post ID |
| `date` | string | Published date (local time, ISO 8601) |
| `date_gmt` | string | Published date (UTC, ISO 8601) |
| `modified` | string | Last modified date (local) |
| `slug` | string | URL slug |
| `link` | string | Full permalink URL |
| `title.rendered` | string | Post title (HTML-decoded) |
| `content.rendered` | string | Full HTML article body |
| `content.protected` | bool | `false` for all public posts |
| `excerpt.rendered` | string | Short HTML excerpt (~1 paragraph) |
| `author` | int | Author user ID |
| `featured_media` | int | Featured image media attachment ID |
| `jetpack_featured_media_url` | string | **Direct URL** to full-res featured image |
| `categories` | int[] | Array of category IDs |
| `tags` | int[] | Array of tag IDs |
| `yoast_head_json.title` | string | SEO page title |
| `yoast_head_json.description` | string | Meta description |
| `yoast_head_json.og_image[0].url` | string | OpenGraph image URL |
| `yoast_head_json.article_published_time` | string | Published time (UTC) |
| `yoast_head_json.article_modified_time` | string | Modified time (UTC) |
| `_embedded.author[0].name` | string | Author name (requires `_embed=author`) |
| `_embedded['wp:featuredmedia'][0].source_url` | string | Featured image URL (requires `_embed=wp:featuredmedia`) |
| `_embedded['wp:term']` | array | Categories & tags as objects `{id, name, slug, taxonomy}` |
### Categories Lookup Endpoint
```
GET https://www.marktechpost.com/wp-json/wp/v2/categories?slug=open-source
```
**Response:**
```json
[
  {
    "id": 11832,
    "name": "Open Source",
    "slug": "open-source",
    "parent": 1447,
    "count": 501
  }
]
```
---
## 5. Approach B — HTML Scraping
### Listing Page URL Pattern
```
Page 1 :  https://www.marktechpost.com/category/technology/open-source/
Page 2+:  https://www.marktechpost.com/category/technology/open-source/page/{N}/
```
- Total pages: **51**
- Posts per page: **100**
### Detecting the Last Page
**Method 1 — Page `<title>` tag:**
```
"Open Source Category - Page 2 of 51 - MarkTechPost"
```
Parse `of {N}` to get the total page count.
**Method 2 — `<link rel="next">` in `<head>`:**
```html
<link rel="next" href="https://www.marktechpost.com/category/technology/open-source/page/3/" />
```
When this element is absent, you are on the last page.
**Method 3 — `<link rel="prev">` and `<link rel="next">`:**
```html
<link rel="prev" href="..." />
<link rel="next" href="..." />
```
---
## 6. Listing Page Structure
The page uses a 12-column grid layout. Blog posts live in the left 8-column section; a sidebar occupies the right 4 columns.
```
.td-main-content-wrap.td-container-wrap
│
├── .td-pb-span8  ← MAIN CONTENT (100 post cards here)
│    └── .td-block-row  (× 50 rows)
│         └── .td-block-span6  (× 2 per row = 100 cards)
│              └── div.td_module_2.td_module_wrap  ← INDIVIDUAL POST CARD
│
└── .td-pb-span4  ← RIGHT SIDEBAR (Recent Articles in .td_module_flex)
     └── .td_module_flex.td_module_wrap  (× 10 posts)
```
> **Important:** Always scope your post card selector to `.td-pb-span8` to exclude the sidebar's `.td_module_flex` posts.
---
## 7. Post Card HTML Structure
Each post card on the listing page follows this structure:
```html
<div class="td_module_2 td_module_wrap td-animation-stack">
  <!-- Thumbnail -->
  <div class="td-module-image">
    <div class="td-module-thumb">
      <a href="{POST_URL}" rel="bookmark" class="td-image-wrap" title="{TITLE}">
        <img class="entry-thumb"
             src="{THUMBNAIL_URL}"
             srcset="..."
             width="324"
             height="160"
             alt="{TITLE}" />
      </a>
    </div>
  </div>
  <!-- Title -->
  <h3 class="entry-title td-module-title">
    <a href="{POST_URL}">{TITLE}</a>
  </h3>
  <!-- Meta -->
  <div class="td-module-meta-info">
    <span class="td-post-author-name">
      <a href="{AUTHOR_URL}">{AUTHOR_NAME}</a>
    </span>
    <span> - </span>
    <span class="td-post-date">
      <time class="entry-date updated td-module-date"
            datetime="{ISO_DATETIME}">
        {HUMAN_DATE}
      </time>
    </span>
    <span class="td-module-comments">
      <a href="{POST_URL}#comments">{COMMENT_COUNT}</a>
    </span>
  </div>
  <!-- Excerpt -->
  <div class="td-excerpt">
    {EXCERPT_TEXT}
  </div>
</div>
```
---
## 8. Post Card Playwright Selectors
```python
# Scope to main content area only (excludes sidebar)
ALL_POST_CARDS  = ".td-pb-span8 .td_module_2.td_module_wrap"
# --- Per card (relative to each card element) ---
# Title text + post URL
TITLE           = "h3.entry-title.td-module-title > a"
# .text_content()         → post title
# .get_attribute("href")  → post URL
# Thumbnail image
THUMBNAIL       = ".td-module-thumb img.entry-thumb"
# .get_attribute("src")   → thumbnail URL (cropped, 324×160)
# Author
AUTHOR_NAME     = ".td-post-author-name > a"
# .text_content()         → author name
# .get_attribute("href")  → author profile URL
# Date
DATE            = "time.entry-date"
# .text_content()              → "March 14, 2026"
# .get_attribute("datetime")   → "2026-03-14T01:44:43-07:00" (ISO 8601)
# Comment count
COMMENTS        = ".td-module-comments > a"
# .text_content()         → "0" (integer as string)
# Excerpt
EXCERPT         = ".td-excerpt"
# .text_content()         → plain text excerpt
```
---
## 9. Individual Post Page Structure
**URL format:**
```
https://www.marktechpost.com/{YYYY}/{MM}/{DD}/{slug}/
```
**Example:**
```
https://www.marktechpost.com/2026/03/05/openai-releases-symphony-an-open-source-agentic-framework.../
```
### Page DOM Hierarchy
```
.td-theme-wrap
└── .td-main-content-wrap.td-container-wrap
     └── .td-main-page-wrap
          ├── .td-pb-span8  ← Article content
          │    ├── .td-post-header          ← Categories + title + meta
          │    │    ├── ul.td-category      ← List of categories
          │    │    ├── .td-post-title
          │    │    │    └── h1.entry-title
          │    │    └── .td-module-meta-info
          │    │         ├── .td-post-author-name > a
          │    │         └── .td-post-date > time.entry-date
          │    ├── .td-post-content         ← Full article HTML body
          │    └── .td-post-tags            ← Tags list
          │
          └── .td-pb-span4  ← Sidebar (related posts, ads)
```
---
## 10. Individual Post Playwright Selectors
```python
# Page title
TITLE           = "h1.entry-title"
# .text_content()
# Categories (multiple)
CATEGORIES      = ".td-post-header ul.td-category li.entry-category a"
# For each: .text_content() → name, .get_attribute("href") → category URL
# Author
AUTHOR_NAME     = ".td-post-author-name a"
# .text_content()
AUTHOR_URL      = ".td-post-author-name a"
# .get_attribute("href")
# Published date
DATE_TEXT       = "time.entry-date"
# .text_content()                 → "March 5, 2026"
DATE_ISO        = "time.entry-date"
# .get_attribute("datetime")      → "2026-03-05T09:37:15-08:00"
# Tags
TAGS            = ".td-post-tags a[rel='tag']"
# For each: .text_content() → tag name, .get_attribute("href") → tag URL
# Full article content (HTML)
ARTICLE_CONTENT = ".td-post-content"
# .inner_html()   → full HTML body
# .text_content() → plain text
# Featured image (fallback — og:image in <head> is more reliable)
OG_IMAGE        = "meta[property='og:image']"
# .get_attribute("content") → full-resolution image URL
# JSON-LD structured data
JSON_LD         = "script[type='application/ld+json']"
# .text_content() → parse as JSON, access [0] for Article schema
```
---
## 11. OpenGraph & JSON-LD Metadata
### OpenGraph Meta Tags (`<head>`)
| Meta Tag | Example Value |
|---|---|
| `og:title` | Post title |
| `og:description` | Short description |
| `og:image` | `https://www.marktechpost.com/wp-content/uploads/.../banner.png` |
| `og:url` | Canonical post URL |
| `og:type` | `article` |
| `article:published_time` | `2026-03-05T17:37:15+00:00` |
| `article:modified_time` | `2026-03-05T17:37:23+00:00` |
| `article:section` | Primary category name |
### JSON-LD Schema.org (`<head>`)
```json
{
  "@context": "https://schema.org",
  "@graph": [
    {
      "@type": "Article",
      "headline": "Post Title Here",
      "datePublished": "2026-03-05T17:37:15+00:00",
      "dateModified": "2026-03-05T17:37:23+00:00",
      "author": {
        "name": "Asif Razzaq"
      },
      "image": {
        "url": "https://www.marktechpost.com/wp-content/uploads/.../image.png"
      },
      "publisher": {
        "name": "MarkTechPost"
      }
    }
  ]
}
```
---
## 12. Playwright Setup & Best Practices
### Browser Initialization
```python
from playwright.async_api import async_playwright
import asyncio
async def create_context():
    p = await async_playwright().start()
    browser = await p.chromium.launch(headless=True)
    context = await browser.new_context(
        user_agent=(
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/122.0.0.0 Safari/537.36"
        )
    )
    return p, browser, context
```
### API Requests via Playwright
```python
async def fetch_api_page(context, page_num: int, per_page: int = 100):
    api_url = (
        "https://www.marktechpost.com/wp-json/wp/v2/posts"
        f"?categories=11832"
        f"&per_page={per_page}"
        f"&page={page_num}"
        "&_embed=author,wp:featuredmedia,wp:term"
        "&_fields=id,date,slug,link,title,content,excerpt,author,"
        "featured_media,categories,tags,jetpack_featured_media_url"
    )
    response = await context.request.get(api_url)
    total = int(response.headers.get("x-wp-total", 0))
    total_pages = int(response.headers.get("x-wp-totalpages", 1))
    data = await response.json()
    return data, total, total_pages
```
### Politeness / Rate Limiting
```python
import asyncio
# Add delay between requests
await asyncio.sleep(1.5)  # 1–3 seconds recommended
```
### Waiting for Elements (HTML scraping)
```python
# Wait for post cards to load on listing page
await page.wait_for_selector(".td-pb-span8 .td_module_2")
# Wait for article content on post page
await page.wait_for_selector(".td-post-content")
```
---
## 13. Recommended Scraping Strategy
### Option 1 — REST API Only (fastest, 6 requests total)
```
1. GET /wp-json/wp/v2/posts?categories=11832&per_page=100&page=1
   → Read X-WP-TotalPages header → N pages
2. Loop page 2 to N:
   GET /wp-json/wp/v2/posts?categories=11832&per_page=100&page={i}
3. Collect all posts from JSON responses
4. All metadata + full HTML content is available in content.rendered
```
**Best for:** Collecting all posts quickly with full content.
---
### Option 2 — HTML Listing Pages Only (51 pages)
```
1. Navigate to /category/technology/open-source/
2. Wait for ".td-pb-span8 .td_module_2"
3. For each post card:
   - Extract title, URL, thumbnail, author, date, excerpt, comment_count
4. Check <link rel="next"> in <head> for next page URL
5. Repeat until no <link rel="next">
```
**Best for:** Collecting post URLs and card-level metadata only.
---
### Option 3 — Hybrid (recommended for complete data)
```
Step 1: Use REST API to collect all 501 post URLs + metadata (6 requests)
Step 2: For each post URL, navigate with Playwright to extract:
        - Full article HTML (.td-post-content)
        - Tags (.td-post-tags a[rel='tag'])
        - JSON-LD structured data
        - OG image URL (meta[property='og:image'])
```
**Best for:** Complete, structured data with full article bodies.
---
## 14. Output Data Schema
```python
post = {
    # Identifiers
    "id":               int,    # WordPress post ID, e.g. 78375
    "slug":             str,    # URL slug
    "url":              str,    # Full permalink
    # Dates
    "published_at":     str,    # ISO 8601, e.g. "2026-03-14T01:44:43"
    "modified_at":      str,    # ISO 8601
    # Content
    "title":            str,    # Plain text title
    "excerpt":          str,    # Short plain text summary
    "content_html":     str,    # Full article HTML body
    # Media
    "featured_image_url": str,  # Full-resolution image URL
    # Taxonomy
    "categories": [
        {"id": int, "name": str, "slug": str, "url": str}
    ],
    "tags": [
        {"id": int, "name": str, "slug": str, "url": str}
    ],
    # Author
    "author": {
        "name": str,
        "url": str              # Author profile page
    },
    # Engagement
    "comment_count":    int,
}
```
---
## 15. Key URLs Reference
| Purpose | URL |
|---|---|
| Open Source category (page 1) | `https://www.marktechpost.com/category/technology/open-source/` |
| Open Source category (page N) | `https://www.marktechpost.com/category/technology/open-source/page/{N}/` |
| REST API — posts | `https://www.marktechpost.com/wp-json/wp/v2/posts` |
| REST API — categories | `https://www.marktechpost.com/wp-json/wp/v2/categories` |
| REST API — Open Source posts | `https://www.marktechpost.com/wp-json/wp/v2/posts?categories=11832` |
| Open Source category ID | `11832` |
| Technology parent category ID | `1447` |
| robots.txt | `https://www.marktechpost.com/robots.txt` |