# Playwright Guide: Extracting a Medium Blog Post to Markdown
## Overview
Medium uses obfuscated CSS class names that change frequently. Reliable selectors use:
1. **Semantic HTML tags** (`h1`, `h2`, `pre`, `blockquote`, `figure`, etc.)
2. **`data-selectable-paragraph`** attribute — present on all editable content blocks
3. **`pw-` prefixed classes** on a few key elements (stable)
4. **`role="separator"`** for visual dividers
---
## Content Types & Selectors
All article body content lives inside: `article section`
### Structure Elements
| Content Type     | Selector                                      | Notes |
|------------------|-----------------------------------------------|-------|
| Title (H1)       | `h1[data-selectable-paragraph]`               | Also has `pw-post-title` class |
| Subtitle         | `h2.pw-subtitle-paragraph`                    | Optional; directly follows H1 |
| H2 Heading       | `h2[data-selectable-paragraph]`               | Section headings |
| H3 Heading       | `h3[data-selectable-paragraph]`               | Sub-section headings |
| H4 Heading       | `h4[data-selectable-paragraph]`               | Rarely used |
### Body Content
| Content Type     | Selector                                      | Notes |
|------------------|-----------------------------------------------|-------|
| Paragraph        | `p[data-selectable-paragraph]`                | Also has `pw-post-body-paragraph` class |
| Bullet List Item | `li[data-selectable-paragraph]`               | Inside `ul` |
| Ordered List Item| `li[data-selectable-paragraph]`               | Inside `ol` |
| Code Block       | `pre`                                         | Contains `span.hljs-*` for syntax highlighting; use `innerText` |
| Blockquote       | `blockquote`                                  | Does NOT have `data-selectable-paragraph` itself; the inner `p` does |
| Separator / HR   | `[role="separator"]`                          | Rendered as `· · ·` dots; no `<hr>` tag is used |
### Images & Media
| Content Type     | Selector                                      | Notes |
|------------------|-----------------------------------------------|-------|
| Inline Image     | `figure.paragraph-image img`                  | Inside `<picture>` with srcset |
| Figure Caption   | `figcaption[data-selectable-paragraph]`       | Has `data-selectable-paragraph`; may be absent |
| Cover Image      | `article > div > figure img`                  | Hero/thumbnail before the article section |
### Inline Formatting (within `p`, `li`, `blockquote`)
| Type              | Selector   | Markdown      |
|-------------------|------------|---------------|
| Bold              | `strong`   | `**text**`    |
| Italic            | `em`       | `_text_`      |
| Inline Code       | `code`     | `` `text` ``  |
| Hyperlink         | `a`        | `[text](url)` |
| Highlighted Text  | `mark`     | `==text==` or `**text**` |
### Metadata & Decorative (skip or handle specially)
| Element          | Selector                          | Notes |
|------------------|-----------------------------------|-------|
| Link Preview Card| `a:has(h2)` (non-medium links)    | Promo cards embedded in body; convert to blockquote link |
| Top Highlight label | `aside`                        | UI label ("Top highlight") — skip, it's not article content |
| Author Name      | `a[href*="post_author_info"] h2`  | Footer area; strip "Written by " prefix |
| Tags             | `a[href*="medium.com/tag/"]`      | At end of article |
---
## DOM Structure Key Facts
### Blockquote
```html
<blockquote class="sb sc sd">
  <p data-selectable-paragraph="">
    "Some quote here." <em>— Author Name</em>
  </p>
</blockquote>
```
The `blockquote` tag itself has no `data-selectable-paragraph`. Target `blockquote` directly by tag.
### Separator / Divider
```html
<div role="separator">
  <span></span>
  <span></span>
  <span></span>
</div>
```
Medium never uses `<hr>`. The visual `· · ·` dots are rendered via CSS on these empty spans. Select with `[role="separator"]`.
### Highlighted Text (`<mark>`)
```html
<p data-selectable-paragraph class="pw-post-body-paragraph ...">
  <mark class="abg abh ak">This sentence was highlighted by readers.</mark>
</p>
```
`<mark>` appears inside `<p>`, `<h1>`, or `<li>`. Convert to `==text==` (Obsidian-compatible) or `**text**`.
### Figcaption
```html
<figure class="... paragraph-image">
  <picture><img src="https://miro.medium.com/..." alt=""></picture>
  <figcaption data-selectable-paragraph="">Photo by Alice on Unsplash</figcaption>
</figure>
```
`figcaption` has `data-selectable-paragraph`. Always check `.catch(() => "")` since it's often absent.
### Ordered List
```html
<ol>
  <li data-selectable-paragraph="">First item with <code>inline code</code></li>
  <li data-selectable-paragraph="">Second item</li>
</ol>
```
Use parent tag to determine `1.` vs `-` prefix.
---
## Complete Playwright Script
```typescript
import { chromium, Page, ElementHandle } from "playwright";
import * as fs from "fs";
// ─── Inline Markdown Helper ──────────────────────────────────────────────────
async function elementToInlineMd(
  page: Page,
  element: ElementHandle
): Promise<string> {
  return page.evaluate((el: Element) => {
    function nodeToMd(node: Node): string {
      if (node.nodeType === Node.TEXT_NODE) {
        return node.textContent || "";
      }
      const el = node as Element;
      const tag = el.tagName.toLowerCase();
      const inner = [...el.childNodes].map(nodeToMd).join("");
      if (tag === "strong" || tag === "b") return `**${inner}**`;
      if (tag === "em" || tag === "i") return `_${inner}_`;
      if (tag === "code") return `\\`${inner}\\``;
      if (tag === "mark") return `**${inner}**`; // highlighted text → bold
      if (tag === "a") {
        const href = (el as HTMLAnchorElement).href;
        // Skip internal Medium tracking links with no meaningful text
        if (!inner.trim()) return "";
        return `[${inner}](${href})`;
      }
      if (tag === "br") return "  \\n"; // markdown line break
      return inner;
    }
    return [...el.childNodes].map(nodeToMd).join("").trim();
  }, element);
}
// ─── Main Extractor ──────────────────────────────────────────────────────────
async function extractMediumArticle(url: string): Promise<string> {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    userAgent:
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
  });
  const page = await context.newPage();
  await page.goto(url, { waitUntil: "domcontentloaded", timeout: 30000 });
  await page.waitForSelector("article section h1[data-selectable-paragraph]", {
    timeout: 15000,
  });
  // Scroll to load lazy images
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
  await page.waitForTimeout(1500);
  await page.evaluate(() => window.scrollTo(0, 0));
  const lines: string[] = [];
  // ── 1. Metadata ─────────────────────────────────────────────────────────
  const title = await page
    .$eval("h1[data-selectable-paragraph]", (el) => el.textContent?.trim() || "")
    .catch(() => "");
  const subtitle = await page
    .$eval("h2.pw-subtitle-paragraph", (el) => el.textContent?.trim() || "")
    .catch(() => "");
  const authorRaw = await page
    .$eval('a[href*="post_author_info"] h2', (el) => el.textContent?.trim() || "")
    .catch(() => "");
  const author = authorRaw.replace(/^Written by\\s+/i, "");
  const tags = await page
    .$$eval('a[href*="medium.com/tag/"]', (els) =>
      [...new Set(els.map((el) => el.textContent?.trim() || "").filter(Boolean))]
    )
    .catch(() => [] as string[]);
  const publishDate = await page
    .evaluate(() => {
      const metaDiv = document.querySelector("article section h1")
        ?.nextElementSibling;
      const text = metaDiv?.innerText || "";
      const match = text.match(
        /\\b(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2},?\\s+\\d{4}/i
      );
      return match ? match[0] : "";
    })
    .catch(() => "");
  // ── 2. Cover Image ───────────────────────────────────────────────────────
  const coverImg = await page
    .$eval("article > div > figure img", (img) => ({
      src: (img as HTMLImageElement).src,
      alt: (img as HTMLImageElement).alt,
    }))
    .catch(() => null);
  // ── 3. Frontmatter ───────────────────────────────────────────────────────
  lines.push("---");
  lines.push(`title: "${title}"`);
  if (subtitle) lines.push(`subtitle: "${subtitle}"`);
  if (author) lines.push(`author: "${author}"`);
  if (publishDate) lines.push(`date: "${publishDate}"`);
  lines.push(`url: "${url}"`);
  if (tags.length) lines.push(`tags: [${tags.map((t) => `"${t}"`).join(", ")}]`);
  lines.push("---");
  lines.push("");
  // ── 4. Title & Subtitle ──────────────────────────────────────────────────
  if (coverImg?.src) {
    lines.push(`![${coverImg.alt || "cover image"}](${coverImg.src})`);
    lines.push("");
  }
  lines.push(`# ${title}`);
  lines.push("");
  if (subtitle) {
    lines.push(`> ${subtitle}`);
    lines.push("");
  }
  // ── 5. Body Content ──────────────────────────────────────────────────────
  // Collect all body content in document order.
  // Selector captures every block-level element type Medium uses.
  const bodySelector = [
    "article section [data-selectable-paragraph]",
    "article section pre",
    "article section figure.paragraph-image",
    "article section blockquote",
    "article section [role='separator']",
  ].join(", ");
  const contentElements = await page.$$(bodySelector);
  // Track visited elements to avoid double-processing nested ones
  // (e.g. a <p> inside <blockquote> will appear both as a blockquote child and a standalone)
  const visitedIds = new Set<string>();
  for (const el of contentElements) {
    const info = await el.evaluate((e: Element) => ({
      tag: e.tagName.toLowerCase(),
      id: e.id || "",
      role: e.getAttribute("role") || "",
      isInsideBlockquote: !!e.closest("blockquote"),
      isInsidePre: !!e.closest("pre"),
      isFigcaption: e.tagName.toLowerCase() === "figcaption",
      isSubtitle: e.classList.contains("pw-subtitle-paragraph"),
      parentTag: e.parentElement?.tagName.toLowerCase() || "",
      listType: e.closest("ul") ? "ul" : e.closest("ol") ? "ol" : "",
      listIndex: (() => {
        if (e.tagName.toLowerCase() !== "li") return 0;
        const siblings = [...(e.parentElement?.querySelectorAll("li") || [])];
        return siblings.indexOf(e as HTMLLIElement);
      })(),
      isLastInList: e.tagName.toLowerCase() === "li" && !e.nextElementSibling,
    }));
    // Skip elements nested inside a <pre> (handled via the pre tag itself)
    if (info.isInsidePre && info.tag !== "pre") continue;
    // Skip the subtitle — already handled
    if (info.isSubtitle) continue;
    // Deduplicate by element ID
    const elId = await el.evaluate((e) => e.id || Math.random().toString());
    if (visitedIds.has(elId)) continue;
    visitedIds.add(elId);
    const { tag, role } = info;
    // ── Separator / Divider ──
    if (role === "separator") {
      lines.push("\\n---\\n");
      continue;
    }
    // ── H1 (skip — already output as title) ──
    if (tag === "h1") continue;
    // ── H2 Heading ──
    if (tag === "h2") {
      const text = await el.evaluate((e) => e.textContent?.trim() || "");
      lines.push(`## ${text}`);
      lines.push("");
      continue;
    }
    // ── H3 Heading ──
    if (tag === "h3") {
      const text = await el.evaluate((e) => e.textContent?.trim() || "");
      lines.push(`### ${text}`);
      lines.push("");
      continue;
    }
    // ── H4 Heading ──
    if (tag === "h4") {
      const text = await el.evaluate((e) => e.textContent?.trim() || "");
      lines.push(`#### ${text}`);
      lines.push("");
      continue;
    }
    // ── Blockquote ──
    if (tag === "blockquote") {
      // The <p> inside the blockquote also appears in the selector,
      // so mark its ID as visited to prevent double-output
      const innerIds = await el.evaluate((e) => {
        return [...e.querySelectorAll("[data-selectable-paragraph]")].map(
          (child) => child.id || ""
        );
      });
      innerIds.forEach((id) => id && visitedIds.add(id));
      // Render blockquote content inline
      const md = await elementToInlineMd(page, el);
      const quoted = md
        .split("\\n")
        .map((l) => `> ${l}`)
        .join("\\n");
      lines.push(quoted);
      lines.push("");
      continue;
    }
    // ── Paragraphs ──
    if (tag === "p") {
      // Skip if it's inside a blockquote (handled above)
      if (info.isInsideBlockquote) continue;
      const md = await elementToInlineMd(page, el);
      if (md.trim()) {
        lines.push(md);
        lines.push("");
      }
      continue;
    }
    // ── List Items ──
    if (tag === "li") {
      const md = await elementToInlineMd(page, el);
      const prefix = info.listType === "ol" ? `${info.listIndex + 1}.` : "-";
      lines.push(`${prefix} ${md}`);
      if (info.isLastInList) lines.push("");
      continue;
    }
    // ── Code Blocks (pre) ──
    if (tag === "pre") {
      const code = await el.evaluate((e) => (e as HTMLElement).innerText || "");
      // Try to detect language from hljs class names
      const lang = await el
        .evaluate((e) => {
          const spans = e.querySelectorAll("[class*='hljs-']");
          if (!spans.length) return "";
          // Look for the language hint on the pre or code element
          const cls = Array.from(e.classList).find((c) => c.startsWith("language-"));
          return cls ? cls.replace("language-", "") : "";
        })
        .catch(() => "");
      lines.push(`\\`\\`\\`${lang}`);
      lines.push(code.trimEnd());
      lines.push("```");
      lines.push("");
      continue;
    }
    // ── Images (figure.paragraph-image) ──
    if (tag === "figure") {
      const imgData = await el
        .$eval("img", (img) => ({
          src: (img as HTMLImageElement).src,
          alt: (img as HTMLImageElement).alt,
          // Get highest-res from srcset if available
          srcset: (img as HTMLImageElement).getAttribute("srcset") || "",
        }))
        .catch(() => null);
      if (!imgData?.src) continue;
      // Pick highest resolution from srcset (largest width descriptor)
      let bestSrc = imgData.src;
      if (imgData.srcset) {
        const entries = imgData.srcset.split(",").map((s) => {
          const [url, w] = s.trim().split(/\\s+/);
          return { url, width: parseInt(w || "0") };
        });
        const best = entries.sort((a, b) => b.width - a.width)[0];
        if (best?.url) bestSrc = best.url;
      }
      const caption = await el
        .$eval("figcaption", (fc) => fc.textContent?.trim() || "")
        .catch(() => "");
      lines.push(`![${imgData.alt || caption || "image"}](${bestSrc})`);
      if (caption) lines.push(`*${caption}*`);
      lines.push("");
      continue;
    }
    // ── Figcaption (standalone — already handled inside figure above) ──
    if (tag === "figcaption") continue;
  }
  // ── 6. Tags Footer ───────────────────────────────────────────────────────
  if (tags.length) {
    lines.push("---");
    lines.push(`**Tags:** ${tags.join(" · ")}`);
    lines.push("");
  }
  await browser.close();
  return lines.join("\\n");
}
// ─── Entry Point ─────────────────────────────────────────────────────────────
(async () => {
  const url = process.argv[2];
  if (!url) {
    console.error("Usage: npx ts-node extract.ts <medium-url>");
    process.exit(1);
  }
  console.log(`Extracting: ${url}`);
  const markdown = await extractMediumArticle(url);
  const filename = `output_${Date.now()}.md`;
  fs.writeFileSync(filename, markdown, "utf-8");
  console.log(`✓ Saved to ${filename}`);
})();
```
---
## Complete Content Type Reference
Observed across 4 articles (tech, engineering, personal essay, editorial):
### Found in ALL articles
- `h1[data-selectable-paragraph]` — title
- `p[data-selectable-paragraph]` — body paragraphs
- `h2[data-selectable-paragraph]` — section headings
- `figure.paragraph-image` + `img` — inline images
- `li[data-selectable-paragraph]` in `ul` — bullet lists
### Found in SOME articles
| Type | Selector | Article Type |
|------|----------|--------------|
| Subtitle | `h2.pw-subtitle-paragraph` | Editorial, essay |
| H3 sub-heading | `h3[data-selectable-paragraph]` | Technical / engineering |
| Blockquote | `blockquote` | Essay / opinion |
| Figcaption | `figcaption[data-selectable-paragraph]` | Any with photos |
| Section separator | `[role="separator"]` | Any longer article |
| Code block | `pre` | Technical |
| Inline code | `code` inside `p` or `li` | Technical |
| Bold | `strong` inside `p` | Editorial |
| Italic | `em` inside `p`, `blockquote` | Essay |
| Highlighted text | `mark` inside `p` or `h1` | Editorial / featured |
| Ordered list | `ol > li[data-selectable-paragraph]` | Tutorial / how-to |
| Link preview card | `a:has(h2)` (non-medium href) | Technical / promo |
| Cover image | `article > div > figure img` | Articles with hero image |
### NOT found (but known to exist in some articles)
| Type | Notes |
|------|-------|
| Twitter/X embed | `iframe[src*="twitter"], div[data-testid*="tweet"]` |
| YouTube embed | `iframe[src*="youtube"]` |
| GitHub Gist | `script[src*="gist.github.com"]` inside a figure |
| `<hr>` tag | Medium does NOT use `<hr>` — always uses `[role="separator"]` instead |
| `<table>` | Rare; Medium editor has limited table support |
---
## Handling Edge Cases
### Blockquote with attribution
```text
> "The quote text here." _— Author Name_
```
The `<em>` inside the blockquote's `<p>` will naturally be converted by `elementToInlineMd`.
### Separator rendering
Medium's `[role="separator"]` renders as three dots `· · ·` visually. The DOM has 3 empty `<span>` elements — use `---` in markdown.
### `<mark>` (reader highlights)
Represents text that many readers highlighted. Since markdown has no native highlight syntax, convert to `**bold**` or `==highlight==` (Obsidian syntax).
### Duplicate content guard
The `[data-selectable-paragraph]` query will match `<p>` tags that are *inside* `<blockquote>` tags. The script tracks visited element IDs to prevent double-rendering.
### Member-only (paywalled) articles
```typescript
// Save login cookies to a file after manual login:
await context.storageState({ path: "auth.json" });
// Reuse in subsequent runs:
const context = await browser.newContext({ storageState: "auth.json" });
```
---
## Setup
```bash
npm install playwright typescript ts-node @types/node
npx playwright install chromium
npx ts-node extract.ts https://pyquantlab.medium.com/regime-based-strategy-...
```