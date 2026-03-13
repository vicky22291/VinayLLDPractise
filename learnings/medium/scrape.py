#!/usr/bin/env python3
"""Medium blog scraper — downloads all free articles from registered blogs as local Markdown.

Usage:
    python scrape.py --register https://bytes.swiggy.com/
    python scrape.py --unregister https://bytes.swiggy.com/
    python scrape.py --list
    python scrape.py                  # scrape all registered blogs
    python scrape.py --only swiggy    # scrape only blogs whose name contains "swiggy"
"""

import argparse
import hashlib
import json
import re
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlparse

import requests
from playwright.sync_api import sync_playwright, Page

SCRIPT_DIR = Path(__file__).resolve().parent
BLOGS_PATH = SCRIPT_DIR / "blogs.json"

USER_AGENT = (
    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
)


# ── Blog registry ───────────────────────────────────────────────────────────

def load_blogs() -> list[dict]:
    if BLOGS_PATH.exists():
        return json.loads(BLOGS_PATH.read_text())
    return []


def save_blogs(blogs: list[dict]):
    BLOGS_PATH.write_text(json.dumps(blogs, indent=2))


def url_to_blog_dir(url: str) -> str:
    """Derive a directory name from a blog URL."""
    parsed = urlparse(url)
    host = parsed.hostname or ""
    # For medium.com/publication-name, use the publication name
    if host == "medium.com":
        path = parsed.path.strip("/").split("/")[0]
        return path or "medium"
    # For custom domains, use the hostname
    return host.replace(".", "-")


def register_blog(url: str):
    url = url.rstrip("/") + "/"
    blogs = load_blogs()
    if any(b["url"] == url for b in blogs):
        print(f"Already registered: {url}")
        return
    name = url_to_blog_dir(url)
    blogs.append({"url": url, "name": name})
    save_blogs(blogs)
    print(f"Registered: {url} -> {name}/")


def unregister_blog(url: str):
    url = url.rstrip("/") + "/"
    blogs = load_blogs()
    new_blogs = [b for b in blogs if b["url"] != url]
    if len(new_blogs) == len(blogs):
        print(f"Not found: {url}")
        return
    save_blogs(new_blogs)
    print(f"Unregistered: {url}")


def list_blogs():
    blogs = load_blogs()
    if not blogs:
        print("No blogs registered. Use --register <url> to add one.")
        return
    for b in blogs:
        print(f"  {b['name']:30s} {b['url']}")


# ── Manifest helpers ─────────────────────────────────────────────────────────

def load_manifest(path: Path) -> dict:
    if path.exists():
        return json.loads(path.read_text())
    return {"fetched": {}}


def save_manifest(path: Path, manifest: dict):
    path.write_text(json.dumps(manifest, indent=2))


# ── URL / slug helpers ───────────────────────────────────────────────────────

def url_to_slug(url: str) -> str:
    """Extract a filename-safe slug from a Medium blog URL."""
    path = urlparse(url).path.strip("/")
    # For medium.com/publication/article-slug, drop the publication prefix
    parts = path.split("/")
    if len(parts) > 1:
        path = parts[-1]
    slug = re.sub(r"[^a-z0-9-]", "-", path.lower())
    slug = re.sub(r"-+", "-", slug).strip("-")
    return slug or "untitled"


def is_article_url(href: str, blog_domain: str) -> bool:
    """Return True if href looks like a Medium article for the given domain."""
    if not href:
        return False
    parsed = urlparse(href)
    host = parsed.hostname or ""
    if blog_domain not in host:
        return False
    path = parsed.path.strip("/")
    # For medium.com/publication URLs, strip the publication prefix
    if host == "medium.com":
        parts = path.split("/")
        if len(parts) < 2:
            return False
        path = parts[-1]
    if not path or path.startswith("tagged/") or path in ("about", "archive", "followers"):
        return False
    if "/" in path and host != "medium.com":
        return False
    if re.search(r"[0-9a-f]{8,}$", path):
        return True
    return False


# ── Image downloading ────────────────────────────────────────────────────────

def download_image(src: str, images_dir: Path, cookies: list[dict] | None = None) -> str | None:
    """Download an image, save to images/ with a hash-based name. Returns relative path."""
    if not src or src.startswith("data:"):
        return None
    try:
        headers = {"User-Agent": USER_AGENT}
        cookie_str = "; ".join(f"{c['name']}={c['value']}" for c in (cookies or []))
        if cookie_str:
            headers["Cookie"] = cookie_str
        resp = requests.get(src, headers=headers, timeout=15)
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
    dest = images_dir / name
    if not dest.exists():
        dest.write_bytes(resp.content)
    return f"../images/{name}"


# ── Inline markdown conversion (runs in browser) ────────────────────────────

INLINE_MD_JS = """
(el) => {
    function nodeToMd(node) {
        if (node.nodeType === Node.TEXT_NODE) return node.textContent || "";
        const tag = node.tagName.toLowerCase();
        const inner = [...node.childNodes].map(nodeToMd).join("");
        if (tag === "strong" || tag === "b") return "**" + inner + "**";
        if (tag === "em" || tag === "i") return "_" + inner + "_";
        if (tag === "code") return "`" + inner + "`";
        if (tag === "mark") return "**" + inner + "**";
        if (tag === "a") {
            const href = node.href;
            if (!inner.trim()) return "";
            return "[" + inner + "](" + href + ")";
        }
        if (tag === "br") return "  \\n";
        return inner;
    }
    return [...el.childNodes].map(nodeToMd).join("").trim();
}
"""


# ── Phase 1: Discover article URLs ──────────────────────────────────────────

def discover_articles(page: Page, blog_url: str, blog_domain: str) -> list[str]:
    """Discover article URLs via sitemap, falling back to scroll-based discovery."""
    urls = _discover_via_sitemap(page, blog_url, blog_domain)
    if not urls:
        print("  Sitemap empty or unavailable, falling back to scroll discovery...")
        urls = _discover_via_scroll(page, blog_url, blog_domain)
    return sorted(urls)


def _discover_via_sitemap(page: Page, blog_url: str, blog_domain: str) -> set[str]:
    """Try fetching article URLs from the sitemap."""
    sitemap_url = blog_url.rstrip("/") + "/sitemap/sitemap.xml"
    try:
        page.goto(sitemap_url, wait_until="domcontentloaded", timeout=30000)
        page.wait_for_timeout(2000)
    except Exception:
        return set()

    content = page.content()
    all_locs = re.findall(r"<loc>(.*?)</loc>", content)

    urls: set[str] = set()
    for loc in all_locs:
        clean = loc.split("?")[0].split("#")[0].rstrip("/")
        if is_article_url(clean, blog_domain):
            urls.add(clean)

    if urls:
        print(f"  Discovered {len(urls)} article URLs from sitemap")
    return urls


def _discover_via_scroll(page: Page, blog_url: str, blog_domain: str) -> set[str]:
    """Scroll the blog homepage to collect article URLs."""
    page.goto(blog_url, wait_until="domcontentloaded", timeout=30000)
    page.wait_for_timeout(3000)

    urls: set[str] = set()
    last_count = 0
    no_change_rounds = 0

    while no_change_rounds < 5:
        hrefs = page.eval_on_selector_all("a[href]", "els => els.map(e => e.href)")
        for href in hrefs:
            clean = href.split("?")[0].split("#")[0].rstrip("/")
            if is_article_url(clean, blog_domain):
                urls.add(clean)

        if len(urls) == last_count:
            no_change_rounds += 1
        else:
            no_change_rounds = 0
            last_count = len(urls)

        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        page.wait_for_timeout(2000)

    print(f"  Discovered {len(urls)} article URLs from scrolling")
    return urls


# ── Phase 2: Extract a single article ───────────────────────────────────────

def extract_article(page: Page, url: str, cookies: list[dict], images_dir: Path) -> str:
    """Navigate to an article URL and extract it as Markdown."""
    page.goto(url, wait_until="domcontentloaded", timeout=30000)

    # Wait for article content — try selector, fall back to fixed wait
    try:
        page.wait_for_selector(
            "article section h1[data-selectable-paragraph], article h1",
            timeout=30000
        )
    except Exception:
        page.wait_for_timeout(5000)
        if not page.query_selector("article h1"):
            raise
    except Exception:
        raise

    page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
    page.wait_for_timeout(1500)
    page.evaluate("window.scrollTo(0, 0)")

    # Detect rendering mode
    has_data_selectable = page.query_selector("[data-selectable-paragraph]") is not None

    lines: list[str] = []

    # ── Metadata ──
    title = _safe_text(page, "h1[data-selectable-paragraph]") if has_data_selectable else _safe_text(page, "article h1")
    subtitle = _safe_text(page, "h2.pw-subtitle-paragraph")
    author_raw = _safe_text(page, 'a[href*="post_author_info"] h2')
    author = re.sub(r"^Written by\s+", "", author_raw, flags=re.I) if author_raw else ""
    tags = _safe_eval(page, 'a[href*="medium.com/tag/"]',
                      "els => [...new Set(els.map(e => (e.textContent || '').trim()).filter(Boolean))]",
                      default=[])
    publish_date = _safe_eval(page, "article section h1", """
        els => {
            const metaDiv = els[0]?.nextElementSibling;
            const text = metaDiv?.innerText || "";
            const m = text.match(/\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4}/i);
            return m ? m[0] : "";
        }
    """, default="")

    # ── Cover image ──
    cover_src = None
    cover_data = None
    try:
        cover_data = page.eval_on_selector(
            "article > div > figure img",
            "img => ({src: img.src, alt: img.alt})"
        )
        if cover_data and cover_data.get("src"):
            local_path = download_image(cover_data["src"], images_dir)
            cover_src = local_path or cover_data["src"]
    except Exception:
        pass

    # ── Frontmatter ──
    lines.append("---")
    lines.append(f'title: "{_escape_yaml(title)}"')
    if subtitle:
        lines.append(f'subtitle: "{_escape_yaml(subtitle)}"')
    if author:
        lines.append(f'author: "{_escape_yaml(author)}"')
    if publish_date:
        lines.append(f'date: "{publish_date}"')
    lines.append(f'url: "{url}"')
    if tags:
        lines.append(f'tags: [{", ".join(f"{t!r}" for t in tags)}]')
    lines.append("---")
    lines.append("")

    # ── Title & subtitle ──
    if cover_src:
        alt = (cover_data or {}).get("alt", "cover image")
        lines.append(f"![{alt}]({cover_src})")
        lines.append("")
    lines.append(f"# {title}")
    lines.append("")
    if subtitle:
        lines.append(f"> {subtitle}")
        lines.append("")

    # ── Body content ──
    if has_data_selectable:
        body_selector = ", ".join([
            "article section [data-selectable-paragraph]",
            "article section pre",
            "article section figure.paragraph-image",
            "article section blockquote",
            "article section [role='separator']",
        ])
    else:
        body_selector = ", ".join([
            "article section p",
            "article section h2",
            "article section h3",
            "article section h4",
            "article section pre",
            "article section figure",
            "article section blockquote",
            "article section [role='separator']",
        ])
    elements = page.query_selector_all(body_selector)

    visited: set[str] = set()

    for el in elements:
        info = el.evaluate("""
            e => ({
                tag: e.tagName.toLowerCase(),
                role: e.getAttribute("role") || "",
                isInsideBlockquote: !!e.closest("blockquote"),
                isInsidePre: !!e.closest("pre"),
                isSubtitle: e.classList.contains("pw-subtitle-paragraph"),
                listType: e.closest("ul") ? "ul" : e.closest("ol") ? "ol" : "",
                listIndex: (() => {
                    if (e.tagName.toLowerCase() !== "li") return 0;
                    const siblings = [...(e.parentElement?.querySelectorAll("li") || [])];
                    return siblings.indexOf(e);
                })(),
                isLastInList: e.tagName.toLowerCase() === "li" && !e.nextElementSibling,
            })
        """)

        tag = info["tag"]

        if info["isInsidePre"] and tag != "pre":
            continue
        if info["isSubtitle"]:
            continue

        # Skip noise elements in old Medium rendering
        if not has_data_selectable and tag == "p":
            text = el.evaluate("e => (e.textContent || '').trim()")
            if text in ("--", "Listen", "Share", "") or len(text) <= 2:
                continue

        el_id = el.evaluate("e => e.id || Math.random().toString()")
        if el_id in visited:
            continue
        visited.add(el_id)

        if info["role"] == "separator":
            lines.append("")
            lines.append("---")
            lines.append("")
            continue

        if tag == "h1":
            continue

        if tag == "h2":
            text = el.evaluate("e => (e.textContent || '').trim()")
            if "stories in\xa0your\xa0inbox" in text or "stories in your inbox" in text:
                continue
            lines.append(f"## {text}")
            lines.append("")
            continue

        if tag == "h3":
            text = el.evaluate("e => (e.textContent || '').trim()")
            lines.append(f"### {text}")
            lines.append("")
            continue

        if tag == "h4":
            text = el.evaluate("e => (e.textContent || '').trim()")
            lines.append(f"#### {text}")
            lines.append("")
            continue

        if tag == "blockquote":
            inner_ids = el.evaluate("""
                e => [...e.querySelectorAll("[data-selectable-paragraph]")].map(c => c.id || "")
            """)
            for iid in inner_ids:
                if iid:
                    visited.add(iid)
            md = el.evaluate(INLINE_MD_JS)
            quoted = "\n".join(f"> {line}" for line in md.split("\n"))
            lines.append(quoted)
            lines.append("")
            continue

        if tag == "p":
            if info["isInsideBlockquote"]:
                continue
            md = el.evaluate(INLINE_MD_JS)
            if md.strip():
                lines.append(md)
                lines.append("")
            continue

        if tag == "li":
            md = el.evaluate(INLINE_MD_JS)
            prefix = f"{info['listIndex'] + 1}." if info["listType"] == "ol" else "-"
            lines.append(f"{prefix} {md}")
            if info["isLastInList"]:
                lines.append("")
            continue

        if tag == "pre":
            code = el.evaluate("e => e.innerText || ''")
            lang = ""
            try:
                lang = el.evaluate("""
                    e => {
                        const cls = Array.from(e.classList).find(c => c.startsWith("language-"));
                        return cls ? cls.replace("language-", "") : "";
                    }
                """)
            except Exception:
                pass
            lines.append(f"```{lang}")
            lines.append(code.rstrip())
            lines.append("```")
            lines.append("")
            continue

        if tag == "figure":
            try:
                img_data = el.eval_on_selector("img", """
                    img => ({
                        src: img.src,
                        alt: img.alt,
                        srcset: img.getAttribute("srcset") || ""
                    })
                """)
            except Exception:
                continue
            if not img_data or not img_data.get("src"):
                continue

            best_src = img_data["src"]
            if img_data.get("srcset"):
                entries = []
                for part in img_data["srcset"].split(","):
                    pieces = part.strip().split()
                    if len(pieces) >= 2:
                        w = int(re.sub(r"\D", "", pieces[1]) or "0")
                        entries.append((pieces[0], w))
                if entries:
                    entries.sort(key=lambda x: x[1], reverse=True)
                    best_src = entries[0][0]

            local_path = download_image(best_src, images_dir, cookies)
            display_src = local_path or best_src

            caption = ""
            try:
                caption = el.eval_on_selector("figcaption", "fc => (fc.textContent || '').trim()")
            except Exception:
                pass

            alt = img_data.get("alt") or caption or "image"
            lines.append(f"![{alt}]({display_src})")
            if caption:
                lines.append(f"*{caption}*")
            lines.append("")
            continue

        if tag == "figcaption":
            continue

    if tags:
        lines.append("---")
        lines.append(f"**Tags:** {' · '.join(tags)}")
        lines.append("")

    return "\n".join(lines)


# ── Phase 3: Internal link resolution ───────────────────────────────────────

def resolve_internal_links(articles_dir: Path, manifest: dict, blog_domain: str):
    """Second pass: replace blog links with local paths where possible."""
    fetched = manifest.get("fetched", {})
    slug_set = {info["slug"] for info in fetched.values()}

    domain_re = re.escape(blog_domain)

    for md_file in articles_dir.glob("*.md"):
        content = md_file.read_text()
        original = content

        def replace_link(m):
            full_url = m.group(1)
            slug = url_to_slug(full_url)
            if slug in slug_set:
                return f"](./{slug}.md)"
            return m.group(0)

        content = re.sub(
            rf"\]\((https?://{domain_re}/[^)]+)\)",
            replace_link,
            content
        )
        if content != original:
            md_file.write_text(content)


# ── Helpers ──────────────────────────────────────────────────────────────────

def _safe_text(page: Page, selector: str) -> str:
    try:
        return page.eval_on_selector(selector, "e => (e.textContent || '').trim()")
    except Exception:
        return ""


def _safe_eval(page: Page, selector: str, js: str, default=None):
    try:
        return page.eval_on_selector_all(selector, js)
    except Exception:
        return default


def _escape_yaml(s: str) -> str:
    return s.replace('"', '\\"')


# ── Scrape a single blog ────────────────────────────────────────────────────

def scrape_blog(page: Page, blog: dict):
    """Scrape all new articles from a single registered blog."""
    blog_url = blog["url"]
    blog_name = blog["name"]
    parsed = urlparse(blog_url)
    blog_domain = parsed.hostname or ""

    blog_dir = SCRIPT_DIR / blog_name
    articles_dir = blog_dir / "articles"
    images_dir = blog_dir / "images"
    manifest_path = blog_dir / "manifest.json"

    articles_dir.mkdir(parents=True, exist_ok=True)
    images_dir.mkdir(parents=True, exist_ok=True)

    manifest = load_manifest(manifest_path)
    fetched = manifest.setdefault("fetched", {})

    # Phase 1: discover
    all_urls = discover_articles(page, blog_url, blog_domain)
    new_urls = [u for u in all_urls if u not in fetched]
    print(f"  New: {len(new_urls)}, skipping: {len(all_urls) - len(new_urls)}")

    if not new_urls:
        print("  Nothing new.")
        return

    cookies = page.context.cookies()

    # Phase 2: extract
    success_count = 0
    for i, url in enumerate(new_urls, 1):
        slug = url_to_slug(url)
        print(f"  [{i}/{len(new_urls)}] {slug}")
        try:
            md = extract_article(page, url, cookies, images_dir)
            (articles_dir / f"{slug}.md").write_text(md)
            fetched[url] = {
                "slug": slug,
                "fetchedAt": datetime.now(timezone.utc).isoformat(),
            }
            save_manifest(manifest_path, manifest)
            success_count += 1
        except Exception as e:
            print(f"    Error: {e}")
        time.sleep(3)

    # Phase 3: resolve internal links
    print("  Resolving internal links...")
    resolve_internal_links(articles_dir, manifest, blog_domain)

    print(f"  Done. {success_count} new, {len(all_urls) - len(new_urls)} skipped.")


# ── Main ─────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Medium blog scraper")
    parser.add_argument("--register", metavar="URL", help="Register a blog URL")
    parser.add_argument("--unregister", metavar="URL", help="Unregister a blog URL")
    parser.add_argument("--list", action="store_true", help="List registered blogs")
    parser.add_argument("--only", metavar="FILTER", help="Only scrape blogs whose name contains FILTER")
    args = parser.parse_args()

    if args.register:
        register_blog(args.register)
        return
    if args.unregister:
        unregister_blog(args.unregister)
        return
    if args.list:
        list_blogs()
        return

    blogs = load_blogs()
    if not blogs:
        print("No blogs registered. Use --register <url> to add one.")
        sys.exit(1)

    if args.only:
        blogs = [b for b in blogs if args.only.lower() in b["name"].lower()]
        if not blogs:
            print(f"No blogs matching '{args.only}'")
            sys.exit(1)

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(user_agent=USER_AGENT)
        page = context.new_page()

        for blog in blogs:
            print(f"\n=== {blog['name']} ({blog['url']}) ===")
            try:
                scrape_blog(page, blog)
            except Exception as e:
                print(f"  Failed: {e}")

        browser.close()

    print("\nAll done.")


if __name__ == "__main__":
    main()
