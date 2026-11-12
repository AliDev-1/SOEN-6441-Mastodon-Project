# Application design

MAWA separates browser actions, data retrieval, and post analysis. The current
implementation includes post representation, JSON conversion, and an asynchronous
REST client, input validation, a shared cache, numbered pagination, readability, and word statistics.
See [the architecture overview](ARCHITECTURE.md) for the diagram and responsibilities.

## Domain model

`Post` is a final Java class with final fields and getters. It stores the post ID,
author display name and account handle, publication `Instant`, language, readable
content, original link, and visibility. Null constructor values are rejected. IDs
remain opaque strings rather than numbers.

There are no JSON, Play, or HTTP dependencies in the model. A later streaming
adapter can produce the same post values without changing the analysis classes.
No actors or reactive streams are implemented in Delivery 1.

## Mapping boundary

`MastodonPostMapper.parse(String)` accepts a complete JSON timeline array and
returns an unmodifiable `List<Post>` in the API's order. Empty arrays are valid.
Malformed JSON, trailing JSON values, non-array bodies, and invalid entries throw
`IllegalArgumentException`; the client translates this into a
controlled application error. One bad entry rejects the response instead of
silently presenting incomplete analysis.

| Mastodon field | Model field | Mapping rule |
| --- | --- | --- |
| `id` | `id` | Required nonblank string; no numeric conversion |
| `account.acct` | `authorHandle` | Required nonblank string |
| `account.display_name` | `authorName` | Missing, null, or blank uses the handle |
| `created_at` | `createdAt` | Required ISO instant, including supported time offsets |
| `language` | `language` | Missing, null, or blank becomes `Unknown` |
| `content` | `content` | Required string; empty media-only content is valid; converted to text |
| `url`, falling back to `uri` | `url` | Missing, null, or blank URL uses URI; chosen link must be absolute HTTP(S) with a host |
| `visibility` | `visibility` | Required nonblank string; preserved for display |

Unexpected JSON types are not coerced into strings. Unused extra fields are ignored.
The selected fields and nullable URL/language behavior follow the official
[Mastodon Status documentation](https://docs.joinmastodon.org/entities/Status/).

The mapper handles top-level status content only. Attachments, custom emoji images,
nested boosts, and quoted-post expansion are outside the current display model.
It does not fetch URLs or impose a timeline request size; the REST client
requests at most 40 posts per page.

## Readable text

`HtmlText.toPlainText(String)` parses HTML locally with jsoup, removes script and
style elements, decodes character entities, and normalizes block boundaries and
whitespace into spaces. Unicode text and ordinary emoji are preserved. Image-only
content has no text; a null fragment returns an empty string.

Plain text is not trusted HTML: Twirl templates escape it normally.
For example, encoded angle brackets become literal text and must not be inserted
through Twirl's raw-HTML helpers. The converter follows jsoup's
[text extraction API](https://jsoup.org/apidocs/org/jsoup/nodes/Element.html#text()).

## Asynchronous REST client

`MastodonClient.fetchPosts(String hashtag, String maxId)` returns a
`CompletionStage<List<Post>>`. Play injects its managed `WSClient`, configuration,
and mapper into a singleton. The client sends one GET to
`/api/v1/timelines/tag/{encodedHashtag}` with `limit=40` and an optional `max_id`.
It makes no blocking waits or automatic retries. Cursor navigation and caching
are handled by `SearchService`. The endpoint and cursor behavior follow
the [official timeline API](https://docs.joinmastodon.org/methods/timelines/).

The request uses an Authorization Bearer header, an Accept JSON header, and a
ten-second timeout. Only configured HTTPS origins are accepted, and redirects are
disabled to keep authorization on that origin. The origin cannot contain user
credentials, an API path, query, or fragment. Configuration is captured when the
client is created; changes require restarting the application.

Null or blank hashtags produce a failed stage with status 400. Full input rules
are enforced by the search service. Nonempty tags are encoded as one path
segment; cursors are passed separately through the HTTP client's query API.

HTTP 200 responses are mapped through `MastodonPostMapper`. Empty arrays are valid;
malformed responses and responses exceeding 40 posts are rejected. Error response
bodies are not read. `MastodonException` carries controlled messages without raw
exceptions, response bodies, or credentials. Controller actions use
its status code to render an appropriate error page.

| Condition | Application status |
| --- | --- |
| Missing or blank hashtag | 400 |
| Missing or invalid instance/token configuration | 503 |
| Upstream 401 or 403 | 502 with an authentication explanation |
| Upstream 429 | 503 with a rate-limit explanation |
| Upstream 503 or a connection failure | 503 |
| Upstream 504 or a transport timeout | 504 |
| Other upstream status or malformed/oversized data | 502 |

Upstream authentication failures do not return 401 to the browser because the
credentials belong to the server configuration, not a browser login session.

## Validated search and cached snapshots

`SearchInput` strips surrounding whitespace, applies Unicode NFC normalization,
and lowercases hashtags with `Locale.ROOT`. This application accepts 1–100 Unicode
letters, marks, numbers, or underscores, including at least one letter. A leading
`#`, embedded whitespace, and punctuation are rejected with 400. These character
and length rules are application decisions, not claims about every valid Mastodon
tag. Cursors remain opaque, case-sensitive strings: null/blank means the first
page; other values allow up to 256 characters without separators or controls.

`SearchService.search` validates before requesting data and returns a
`CompletionStage<SearchPage>`. Its singleton Caffeine async cache keys each page by
configured instance, normalized hashtag, and requested cursor. Atomic loading
shares overlapping requests; failed loads are removed so the next call can retry.
The returned future is a copy, preventing a caller from cancelling the shared load.
No production code blocks on a future.

The cache retains up to 100 completed pages after maintenance. Expiry is ten minutes
after a successful load completes, and reads do not extend it. Pending loads are
shared but are not an admission/concurrency limit; transport timeouts bound their
duration. Cache data lives in memory and disappears when the app restarts. Caffeine
performs size maintenance through a direct executor; callbacks contain only short
in-memory operations. The policy follows Caffeine's official
[population](https://github.com/ben-manes/caffeine/wiki/Population) and
[eviction](https://github.com/ben-manes/caffeine/wiki/Eviction) documentation.

`SearchPage` defensively copies the post list and records the normalized term,
requested cursor, and a unique snapshot ID. `cachedPage(id)` scans the small cache's
completed, unexpired values without waiting or issuing HTTP calls. Missing, expired,
or evicted IDs return 410 with instructions to search again. A refreshed query has
a new ID, so old analysis links cannot silently switch to different posts.

## Browser search flow

The single `ApplicationController` receives injected `SearchService` and exposes
asynchronous actions for `GET /` and `GET /search?hashtag=...`. The search action
passes the browser value to the service and renders its completed snapshot. It
unwraps completion failures, returns controlled error statuses/messages, and uses
a generic 500 response for unexpected exceptions without exposing their details.
The home action does not fetch posts.

Shared Twirl layout and form templates support the home, results, and error pages.
The results template displays posts in API order, all required metadata, UTC times,
and original links. Twirl escapes all text and attributes; no raw HTML is rendered
from posts. Empty successful responses remain HTTP 200 with a no-results message.
A GET form makes search URLs bookmarkable. Error forms retain the submitted term.
Responsive styles, explicit labels, visible focus indicators, and an alert role
make the basic interface usable with keyboard navigation and narrow screens.

## Cursor pagination and cached navigation

`SearchPage.getNextCursor()` returns the last post ID without numeric conversion.
An empty page or an ID equal to the requested cursor returns an empty cursor,
preventing a nonadvancing link. A page with fewer than 40 posts is not assumed to
be the last page. `SearchService.olderPage(id)` first requires the source snapshot
to remain cached, then searches the same hashtag with that cursor. Requests retain
`limit=40`; `max_id` selects older results according to the
[Mastodon hashtag timeline API](https://docs.joinmastodon.org/methods/timelines/#view-hashtag-timeline).

`GET /results/:id/older` redirects to `GET /results/:id` for the resulting snapshot.
The cached route renders the exact retained posts and never refreshes missing data.
A missing or expired snapshot returns 410; manually requesting an older page from
an empty/nonadvancing source returns 400. Upstream failures use the existing safe
error UI. Cursor values are derived on the server rather than accepted from a
pagination link, and no upstream Link URL is followed.

The service finds a predecessor among completed, unexpired pages with the same
hashtag whose final post ID matches the current page's requested cursor. This
supports successive backward navigation without storing a separate history or
changing shared snapshots. The predecessor link disappears when that page is no
longer retained; a link that expires after rendering returns 410. Matching by cursor
can select a newer retained predecessor if the original query was refreshed; the
chosen link always identifies its exact snapshot. The **First page** link runs the
initial search, reusing its cache entry when available. Page numbers are carried as navigation context in the `pageNumber` query parameter,
starting at 1, incrementing for older results, and decrementing for previous links.
They describe the browsing sequence rather than a global position in the changing
remote timeline. They are deliberately separate from cache keys so callers at
different browsing positions can reuse the same response. Saved-page links preserve
the number; old snapshot URLs without it default to page 1. Invalid/nonpositive
numbers are rejected, and integer overflow is prevented. No total page count is
claimed or fetched.

The shared pagination template appears above and below results. Previous/next
buttons surround the current page number; first-page and saved-page utilities sit
on a separate row. A short note explains that the saved link revisits the same
posts temporarily. Unavailable actions remain visible as disabled text. On narrow
screens the page number appears above the navigation buttons.

## Readability and post details

`ReadabilityService` is a stateless calculator with no HTTP dependency. It receives
immutable `Post` values from the retained `SearchPage` and produces `Readability`
measurements. `analyzePage` indexes measurements by post ID for the overview;
`findPost` limits details lookup to the requested snapshot and returns 404 if the
post is absent. It never fetches missing posts from Mastodon.

`GET /results/:id/posts/:postId?pageNumber=...` asynchronously retrieves the snapshot,
looks up the post, and renders its full metadata, readable text, original link,
scores, and counts. An expired/evicted snapshot returns 410. The return link retains
the snapshot ID and browsing position. Overview and details share the same score
template and calculation, with no additional API call. Scores may be recalculated
locally; the source posts are the cached data required by the assignment.

The formulas use `W` words, `S` sentences, and `Y` estimated syllables:

- Flesch Reading Ease = `206.835 - 1.015 × (W / S) - 84.6 × (Y / W)`.
- Flesch–Kincaid Grade Level = `0.39 × (W / S) + 11.8 × (Y / W) - 15.59`.

These coefficients and the English interpretation follow
[Microsoft's readability documentation](https://support.microsoft.com/en-us/word/get-your-document-s-readability-and-level-statistics-in-microsoft-word).
Ratios use floating-point division. Scores retain full precision in the model and
are displayed with two decimals and a locale-independent decimal point. Scores are
not clamped: short text can yield ease above 100 or negative grade levels. Higher
ease generally indicates easier reading; grade level estimates US school grade.

### Counting and language policy

These are application heuristics, not a dictionary-based pronunciation engine:

- Accept `en` and case-insensitive `en-...` language tags. `Unknown` is processed
  with an explicit English-based-estimate label; known other languages are unavailable.
- Remove HTTP(S) and `www.` URLs through the next whitespace before counting.
- Match Unicode letter sequences with internal straight/curly apostrophes as words.
  Hyphens separate words. Hashtag/mention markers are ignored, but their letters
  count as words. Numbers and emoji do not count as words.
- Split sentences on runs of `.`, `!`, or `?`; count only fragments containing words.
  A trailing or unpunctuated fragment with words counts as one sentence.
- Lowercase each word and fold accents, retaining ASCII letters for syllable counting.
  Count groups of `a/e/i/o/u/y`. Subtract one for a final silent `e` when there is more
  than one group, except consonant-plus-`le` endings. Keep at least one syllable.
- If no words remain, return an unavailable result without division by zero. Zero
  placeholder fields in unavailable measurements are never displayed as scores.

Examples: `The cat sat on the mat.` has 6 words, 1 sentence, 6 syllables, ease
116.145 and grade -1.45 before display rounding. `A simple sentence.` has 3 words,
1 sentence, 5 syllables, ease 62.79 and grade approximately 5.246667.

Abbreviations, decimals, names, acronyms, irregular vowel sounds, borrowed words,
and URL-adjacent punctuation can make these counts inaccurate. Unknown-language
metadata is not language detection; mixed-language posts also remain approximate.
Short social posts provide especially weak evidence for a reading grade. The UI
labels scores as estimates and exposes the counting rules on the details page.

## Word statistics with Java Streams

`WordStatisticsService.analyze(SearchPage)` has no network or mutable shared state.
It uses a sequential Java Streams pipeline: map cached posts to plain-text content,
flat-map each content into tokens, group with `Collectors.groupingBy` and
`Collectors.counting`, then map entries to immutable `WordFrequency` values. A
comparator sorts counts descending and breaks ties using natural Java string order.
`WordStatistics` defensively copies the list, reports its size as the unique count,
and uses `mapToLong(...).sum()` for total occurrences.

The tokenizer removes HTTP(S)/www URLs through the next whitespace, converts curly
apostrophes to straight ones, lowercases with `Locale.ROOT`, and applies Unicode
NFC normalization. Words start with a Unicode letter, continue with letters or
combining marks, and may include internal apostrophes followed by another letter.
Accents are retained: `café` and `cafe` are different words, while composed and
decomposed spellings of `café` combine. `CAN’T` and `can't` combine. Hyphens,
underscores, and digits separate letter runs (`a1b_c` gives `a`, `b`, `c`). Standalone
numbers, emoji, and punctuation contribute no words. Letters in hashtags and
mentions count; stop words remain included. No stemming or language-specific
segmentation is performed. Counts include all languages, unlike English-only
readability estimates. Unicode natural order is deterministic but is not a
language-specific alphabetical collation.

`GET /results/:id/words?pageNumber=...` reads the retained snapshot and computes
local statistics. The search term in the results heading links to this route, with
an explanation of its purpose. The view identifies the page scope, shows total
occurrences and unique words, and renders an escaped accessible table. Empty input
is HTTP 200 with zero counts and a no-words message. Invalid page numbers return
400; unavailable snapshots return 410 without refetching. A return link preserves
the snapshot and page number. Reading statistics or returning to results makes no
additional Mastodon request.

For example, posts `Banana apple APPLE the` and `banana cherry the apple` produce
8 total occurrences and 4 unique words: `apple:3`, `banana:2`, `the:2`, `cherry:1`.
The analysis is intentionally limited to the current page (at most 40 posts);
fetching every matching post is neither necessary nor performed.

## Final phase and delivery status

Phase 10 (Coverage and full-flow verification) is recorded in
[VERIFICATION.md](VERIFICATION.md). Phase 11 (Documentation and generated reports)
is the final project phase and provides the architecture diagram and generated reports.
Outstanding clean-checkout, package inspection, and external submission/demo checks
remain tracked in [REQUIREMENTS.md](REQUIREMENTS.md). They are delivery checks,
not a separate implementation phase. Automated tests do not establish completion
of those external deliverables.
