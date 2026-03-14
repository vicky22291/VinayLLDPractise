#!/usr/bin/env python3
"""MarkTechPost Open Source category scraper — downloads all posts as local Markdown.

Uses the WordPress REST API (primary) with optional Playwright HTML fallback.

Usage:
    python scrape.py                  # scrape all Open Source posts via REST API
    python scrape.py --limit 10       # scrape only 10 posts
    python scrape.py --html           # use Playwright HTML scraping instead
"""

import argparse
import hashlib
import json
import re
import sys
import time
from datetime import datetime, timezone
from html import unescape
from pathlib import Path
from urllib.parse import urlparse

import requests

SCRIPT_DIR = Path(__file__).resolve().parent
ARTICLES_DIR = SCRIPT_DIR / "articles"
IMAGES_DIR = SCRIPT_DIR / "images"
MANIFEST_PATH = SCRIPT_DIR / "manifest.json"

BASE_URL = "https://www.marktechpost.com"
API_URL = f"{BASE_URL}/wp-json/wp/v2/posts"
OPEN_SOURCE_CATEGORY_ID = 11832

USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
)

HEADERS = {"User-Agent": USER_AGENT}


# ── Manifest helpers ─────────────────────────────────────────────────────────

def load_manifest() -> dict:
    if MANIFEST_PATH.exists():
        return json.loads(MANIFEST_PATH.read_text())
    return {"fetched": {}}


def save_manifest(manifest: dict):
    MANIFEST_PATH.write_text(json.dumps(manifest, indent=2))


# ── Slug / filename helpers ──────────────────────────────────────────────────

def post_to_slug(post: dict) -> str:
    slug = post.get("slug", "")
    if not slug:
        slug = re.sub(r"[^a-z0-9-]", "-", post.get("title", {}).get("rendered", "untitled").lower())
        slug = re.sub(r"-+", "-", slug).strip("-")
    return slug


# ── Image downloading ────────────────────────────────────────────────────────

def download_image(src: str) -> str | None:
    if not src or src.startswith("data:"):
        return None
    try:
        resp = requests.get(src, headers=HEADERS, timeout=15)
        resp.raise_for_status()
    except Exception as e:
        print(f"  Warning: failed to download image {src[:80]}: {e}")
        return None

    content_type = resp.headers.get("content-type", "")
    ext = ".jpg"
    if "png" in content_type:
        ext = ".png"
    elif "gif" in content_type:
        ext = ".gif"
    elif "webp" in content_type:
        ext = ".webp"
    elif "svg" in content_type:
        ext = ".svg"

    name = hashlib.sha256(src.encode()).hexdigest()[:16] + ext
    dest = IMAGES_DIR / name
    if not dest.exists():
        dest.write_bytes(resp.content)
    return f"../images/{name}"


# ── HTML to Markdown conversion ──────────────────────────────────────────────

def html_to_markdown(html: str) -> str:
    """Convert WordPress HTML content to readable Markdown."""
    text = html

    # Remove script/style tags
    text = re.sub(r"<(script|style)[^>]*>.*?</\1>", "", text, flags=re.DOTALL | re.IGNORECASE)

    # Headings
    for level in range(6, 0, -1):
        text = re.sub(
            rf"<h{level}[^>]*>(.*?)</h{level}>",
            lambda m, l=level: f"\n{'#' * l} {_strip_tags(m.group(1))}\n",
            text, flags=re.DOTALL | re.IGNORECASE,
        )

    # Bold / italic / code
    text = re.sub(r"<(strong|b)>(.*?)</\1>", r"**\2**", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<(em|i)>(.*?)</\1>", r"_\2_", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"<code>(.*?)</code>", r"`\1`", text, flags=re.DOTALL | re.IGNORECASE)

    # Links
    text = re.sub(
        r'<a[^>]+href="([^"]*)"[^>]*>(.*?)</a>',
        lambda m: f"[{_strip_tags(m.group(2))}]({m.group(1)})" if m.group(2).strip() else "",
        text, flags=re.DOTALL | re.IGNORECASE,
    )

    # Images
    text = re.sub(
        r'<img[^>]+src="([^"]*)"[^>]*alt="([^"]*)"[^>]*/?>',
        r"![\2](\1)",
        text, flags=re.IGNORECASE,
    )
    text = re.sub(
        r'<img[^>]+src="([^"]*)"[^>]*/?>',
        r"![image](\1)",
        text, flags=re.IGNORECASE,
    )

    # Pre / code blocks
    text = re.sub(
        r"<pre[^>]*><code[^>]*>(.*?)</code></pre>",
        lambda m: f"\n```\n{unescape(_strip_tags(m.group(1)))}\n```\n",
        text, flags=re.DOTALL | re.IGNORECASE,
    )
    text = re.sub(
        r"<pre[^>]*>(.*?)</pre>",
        lambda m: f"\n```\n{unescape(_strip_tags(m.group(1)))}\n```\n",
        text, flags=re.DOTALL | re.IGNORECASE,
    )

    # Blockquotes
    text = re.sub(
        r"<blockquote[^>]*>(.*?)</blockquote>",
        lambda m: "\n" + "\n".join(f"> {line}" for line in _strip_tags(m.group(1)).strip().split("\n")) + "\n",
        text, flags=re.DOTALL | re.IGNORECASE,
    )

    # Lists
    text = re.sub(r"<li[^>]*>(.*?)</li>", lambda m: f"- {_strip_tags(m.group(1)).strip()}\n", text, flags=re.DOTALL | re.IGNORECASE)
    text = re.sub(r"</?[ou]l[^>]*>", "\n", text, flags=re.IGNORECASE)

    # Paragraphs and line breaks
    text = re.sub(r"<br\s*/?>", "\n", text, flags=re.IGNORECASE)
    text = re.sub(r"<p[^>]*>(.*?)</p>", lambda m: f"\n{m.group(1).strip()}\n", text, flags=re.DOTALL | re.IGNORECASE)

    # Horizontal rules
    text = re.sub(r"<hr[^>]*/?>", "\n---\n", text, flags=re.IGNORECASE)

    # Figure captions
    text = re.sub(r"<figcaption[^>]*>(.*?)</figcaption>", lambda m: f"*{_strip_tags(m.group(1)).strip()}*\n", text, flags=re.DOTALL | re.IGNORECASE)

    # Strip remaining tags
    text = re.sub(r"<[^>]+>", "", text)

    # Decode HTML entities
    text = unescape(text)

    # Clean up whitespace
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def _strip_tags(html: str) -> str:
    return unescape(re.sub(r"<[^>]+>", "", html))


# ── REST API fetching ────────────────────────────────────────────────────────

def fetch_all_posts(limit: int | None = None) -> list[dict]:
    """Fetch all Open Source posts via the WP REST API."""
    all_posts = []
    page_num = 1

    while True:
        params = {
            "categories": OPEN_SOURCE_CATEGORY_ID,
            "per_page": 100,
            "page": page_num,
            "_embed": "author,wp:featuredmedia,wp:term",
            "_fields": "id,date,modified,slug,link,title,content,excerpt,author,"
                       "featured_media,categories,tags,jetpack_featured_media_url",
        }
        print(f"  Fetching API page {page_num}...")
        resp = requests.get(API_URL, params=params, headers=HEADERS, timeout=30)
        resp.raise_for_status()

        total = int(resp.headers.get("X-WP-Total", 0))
        total_pages = int(resp.headers.get("X-WP-TotalPages", 1))
        posts = resp.json()

        if page_num == 1:
            print(f"  Total posts: {total}, pages: {total_pages}")

        all_posts.extend(posts)

        if limit and len(all_posts) >= limit:
            all_posts = all_posts[:limit]
            break

        if page_num >= total_pages:
            break

        page_num += 1
        time.sleep(1.5)

    return all_posts


# ── Post to Markdown ─────────────────────────────────────────────────────────

def post_to_markdown(post: dict) -> str:
    """Convert a WP REST API post object to a Markdown string."""
    title = _strip_tags(post.get("title", {}).get("rendered", "Untitled"))
    excerpt = _strip_tags(post.get("excerpt", {}).get("rendered", "")).strip()
    content_html = post.get("content", {}).get("rendered", "")
    link = post.get("link", "")
    date = post.get("date", "")
    modified = post.get("modified", "")
    slug = post.get("slug", "")

    # Author
    author_name = ""
    embedded = post.get("_embedded", {})
    authors = embedded.get("author", [])
    if authors:
        author_name = authors[0].get("name", "")

    # Categories & tags
    categories = []
    tags = []
    terms = embedded.get("wp:term", [])
    for term_group in terms:
        if not isinstance(term_group, list):
            continue
        for t in term_group:
            if t.get("taxonomy") == "category":
                categories.append(t.get("name", ""))
            elif t.get("taxonomy") == "post_tag":
                tags.append(t.get("name", ""))

    # Featured image
    featured_img = post.get("jetpack_featured_media_url", "")
    if not featured_img:
        media = embedded.get("wp:featuredmedia", [])
        if media:
            featured_img = media[0].get("source_url", "")

    local_img = download_image(featured_img) if featured_img else None

    # Build markdown
    lines = []

    # Frontmatter
    lines.append("---")
    lines.append(f'title: "{_escape_yaml(title)}"')
    if author_name:
        lines.append(f'author: "{_escape_yaml(author_name)}"')
    if date:
        lines.append(f'date: "{date}"')
    if modified:
        lines.append(f'modified: "{modified}"')
    lines.append(f'url: "{link}"')
    lines.append(f'slug: "{slug}"')
    if categories:
        lines.append(f'categories: [{", ".join(repr(c) for c in categories)}]')
    if tags:
        lines.append(f'tags: [{", ".join(repr(t) for t in tags)}]')
    lines.append("---")
    lines.append("")

    # Featured image
    if local_img or featured_img:
        lines.append(f"![{title}]({local_img or featured_img})")
        lines.append("")

    # Title
    lines.append(f"# {title}")
    lines.append("")

    # Excerpt
    if excerpt:
        lines.append(f"> {excerpt}")
        lines.append("")

    # Body
    body_md = html_to_markdown(content_html)
    lines.append(body_md)
    lines.append("")

    # Tags footer
    if tags:
        lines.append("---")
        lines.append(f"**Tags:** {' · '.join(tags)}")
        lines.append("")

    return "\n".join(lines)


def _escape_yaml(s: str) -> str:
    return s.replace('"', '\\"')


# ── Main scraping logic ─────────────────────────────────────────────────────

def scrape(limit: int | None = None):
    ARTICLES_DIR.mkdir(parents=True, exist_ok=True)
    IMAGES_DIR.mkdir(parents=True, exist_ok=True)

    manifest = load_manifest()
    fetched = manifest.setdefault("fetched", {})

    # Fetch posts
    posts = fetch_all_posts(limit=limit)
    new_posts = [p for p in posts if p.get("link", "") not in fetched]
    print(f"\n  Total fetched: {len(posts)}, new: {len(new_posts)}, skipping: {len(posts) - len(new_posts)}")

    if not new_posts:
        print("  Nothing new.")
        return

    # Convert and save
    success = 0
    for i, post in enumerate(new_posts, 1):
        slug = post_to_slug(post)
        link = post.get("link", "")
        print(f"  [{i}/{len(new_posts)}] {slug}")
        try:
            md = post_to_markdown(post)
            (ARTICLES_DIR / f"{slug}.md").write_text(md)
            fetched[link] = {
                "slug": slug,
                "id": post.get("id"),
                "fetchedAt": datetime.now(timezone.utc).isoformat(),
            }
            save_manifest(manifest)
            success += 1
        except Exception as e:
            print(f"    Error: {e}")

    print(f"\n  Done. {success} new articles saved to {ARTICLES_DIR}/")


# ── CLI ──────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="MarkTechPost Open Source blog scraper")
    parser.add_argument("--limit", type=int, default=None, help="Max posts to fetch")
    args = parser.parse_args()

    print("=== MarkTechPost Open Source Scraper ===")
    scrape(limit=args.limit)
    print("\nAll done.")


if __name__ == "__main__":
    main()
