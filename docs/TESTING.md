# Testing strategy

Run `sbt test` for JUnit tests and `sbt jacoco` for tests with coverage. The tests
use an isolated Play application and never contact the live Mastodon API.
Mockito isolates the client and search service from the network.

## Phase 2 cases

| Case | Input or partition | Expected result | Test |
| --- | --- | --- | --- |
| Direct home action | No input parameters | Completed response with status 200, HTML content type, exact title, heading, language, and status text | `indexReturnsHomePage` |
| Home route | `GET /` | Routing and dependency injection return the same specified home page | `homeRouteReturnsHomePage` |
| Stylesheet | Asset URL extracted from the home page | Status 200, CSS content type, expected font rule | `homeStylesheetIsServed` |

The home action has one input partition because it accepts no parameters and has
no conditional branches. Hashtag, API response, cache, and analysis partitions will
be added alongside those features. Tests assert specified values, not just nonempty
responses. Future waits are confined to test code.

## Phase 3 cases

| Input partition | Expected result | Test coverage |
| --- | --- | --- |
| Complete post values; each null constructor argument | Exact getters; field-specific null rejection | `PostTest` |
| Null, empty, whitespace, plain text | Exact normalized output | `HtmlTextTest` cases 0–3 |
| Paragraphs, breaks, entities, Unicode, links | Words remain readable and separated | `HtmlTextTest` cases 4–7 |
| Scripts/styles, broken markup, image-only text, encoded tags | No executable nodes; exact text including literal encoded tags | `HtmlTextTest` cases 8–11 |
| Valid timeline with extra fields and multiple posts | Exact fields, preserved order, immutable list | `mapsValuesInApiOrder` |
| Empty array | Empty list | `acceptsEmptyArray` |
| Missing body, malformed JSON, trailing data, wrong root type | Specific validation error | `rejectsInvalidBodies` |
| Invalid entry after a valid post | Entire response rejected | `rejectsInvalidEntries` |
| Missing, null, numeric, or blank required strings; invalid account | Field-specific error | `rejectsInvalidRequiredFields`, `rejectsInvalidAccounts` |
| Optional values missing, null, blank, or wrong type | Documented defaults or field-specific error | `defaultsOptionalFields`, `rejectsIncorrectOptionalTypes` |
| Empty content, metadata whitespace, timestamp offset | Valid post with exact normalized values | `acceptsEmptyContentAndTrimsMetadata` |
| Malformed or impossible dates | Publication-date error | `rejectsInvalidDates` |
| HTTP, HTTPS, executable/relative/hostless/malformed URLs; missing fallback | Accepted web links or validation error | `validatesOriginalLinks` |

Related partitions are grouped in tests, but invalid fields are changed separately
so one error does not mask another. All JSON is synthetic; no credentials or live
responses are used. Phase 3 verification passed 28 tests and achieved 100% line,
method, and branch coverage of the current handwritten production code.

## Phase 4 cases

`MastodonClientTest` mocks Play's `WSClient`, `WSRequest`, and `WSResponse` using
Mockito. It uses dummy credentials and manually completed futures. Its dependency
injection test also replaces the HTTP client with a mock. No unit test reads `.env`
or calls Mastodon.

| Partition | Expected result | Test |
| --- | --- | --- |
| Pending request, then empty success | Returns immediately; exact endpoint, headers, limit, timeout, redirect policy; empty list after completion | `sendsBoundedNonBlockingRequest` |
| Unicode and reserved path characters; opaque cursor; trailing origin slash | Encoded tag and separate cursor query value | `encodesTagAndAddsCursor` |
| Blank cursor | No `max_id` | `omitsBlankCursor` |
| Null, empty, or whitespace hashtag | Failed stage, 400, no request | `rejectsAbsentTags` |
| 1 and 40 posts | Exact counts and mapped field values | `mapsSuccessfulPages` |
| Missing/malformed body or 41 posts | Safe 502 | `rejectsInvalidResponseData` |
| 401, 403, 429, 503, 504, 500, 404, 302, 204 | Exact mapped status/message; no error-body access | `mapsHttpFailures` |
| Direct and wrapped timeouts | Safe 504 | `mapsTimeoutFailures` |
| Connection or synchronous request failure | Safe 503, no raw cause | `mapsConnectionFailures`, `mapsSynchronousFailures` |
| Invalid origin components or missing settings | Safe 503 before network activity | `rejectsInvalidOrigins` |
| Absent settings or blank token | Safe 503 before network activity | `rejectsMissingConfiguration` |
| Guice-created client | Correct injection and singleton instance | `supportsDependencyInjection` |

`MastodonExceptionTest` separately checks the error status, message, and absent
cause. Phase 4 passed 42 tests with 100% line, method, and branch coverage of the
current handwritten production code.

A separate, temporary manual runner exercised the actual client using local
configuration and successfully mapped 40 `java` posts. It printed only the count,
closed its application, and was removed afterward. Live verification is separate
from the reproducible automated test suite.

## Phase 5 cases

| Partition | Expected behavior |
| --- | --- |
| Null/blank, hash prefix, spaces, punctuation, numeric-only, overlength hashtag | 400 before client invocation |
| Mixed case, equivalent Unicode, non-Latin text, 100-character boundary | Normalized valid hashtag |
| Null/blank cursor, opaque punctuation, 256-character boundary | Valid cursor preserved |
| Internal separators/controls or overlength cursor | 400 before client invocation |
| Repeated normalized search, pending request, eight concurrent callers | One API load and the same snapshot |
| Caller cancellation | Other callers and shared load unaffected |
| Different term or cursor | Separate response snapshots |
| Delayed or immediately failed load | Safe failure; next search retries |
| Empty successful page | Cached normally |
| Ten-minute boundary, reads just before expiry | Snapshot expires without extending its lifetime |
| Unknown/null/expired snapshot, refreshed same query | 410 without fetching replacement analysis data |
| 120 distinct pages | Exactly 100 retained after maintenance |
| Caller mutates source list or returned list | Defensive copy and unmodifiable cached posts |

`SearchServiceTest` uses Mockito and a monotonic fake clock; expiry requires no
sleep. Its concurrency test releases eight workers together and waits only in test
code with timeouts. `SearchPageTest` verifies exact getters, defensive copying,
unmodifiable results, and null rejection. No test reads credentials or calls the
live API. Phase 5 passed 51 tests with 100% line, method, and branch coverage
across all nine handwritten production classes; Javadoc also passed. Actual browser
search and analysis integration remain for later phases.

## Phase 6 cases

`ApplicationControllerTest` checks the updated home form, route, and stylesheet.
`SearchControllerTest` replaces the service with a mock to inspect response HTML
using jsoup. A separate integration case uses the real service with a mocked
Mastodon client to verify validation and repeated-search cache reuse.

| Partition | Expected result |
| --- | --- |
| Two posts with distinct metadata, encoded markup, and empty content | Exact fields/order, escaped text, original URLs, no-text placeholder |
| Empty successful page | 200, zero count, clear no-results message |
| Missing term and controlled errors | Correct 400/410/502/503/504 status and explanation |
| Invalid input containing HTML | Preserved input, no injected elements |
| Pending operation | Action returns an incomplete stage without blocking |
| Unexpected exception or completion wrapper without cause | Generic 500; no raw failure details |
| Real validator with leading hash | 400 without any client call |
| Repeated case-equivalent browser searches | Identical snapshot HTML and one client call |

Phase 6 verification passed 57 tests, 100% line/method/branch coverage of handwritten
production code, and Javadoc generation.

Live browser visual inspection remains pending because the browser host was
unavailable. The existing localhost server returned a missing-dependency compile
error; a restart is needed to pick up Phase 5's build changes. Tests use newly
constructed applications with the current dependencies and no live Mastodon calls.

## Phase 7 cases

| Partition | Expected result |
| --- | --- |
| Empty, short, full, opaque-ID, nonadvancing page | Empty cursor only for empty/nonadvancing pages; otherwise exact final ID |
| Multiple older requests from the same source | Same cached snapshot and one client request per cursor |
| Multi-page predecessor chain | Same-hashtag predecessors found without fetching |
| Other hashtag, unmatched cursor, expired predecessor | No predecessor link |
| Missing/expired source snapshot | 410 before any API request |
| Older request from an empty source | 400 without another client call |
| Rendered older link, redirect, and cached-page route | 303 then 200, distinct empty older-page message |
| Previous cached link and repeated older link | Exact original HTML and no duplicate client calls |
| Both navigation actions fail | Controlled error page and appropriate status |

`SearchPageTest` covers cursor boundaries. `SearchServiceTest` uses its fake clock
for expiry and verifies exact calls. `SearchControllerTest` follows rendered links
through real routing and caching with a mocked Mastodon client. Phase 7, including navigation refinements, passed 64
tests, 100% line/method/branch coverage of handwritten production code, and Javadoc.
Numbered-navigation tests verify the top and bottom page indicators, position
preservation in previous/next/saved links, disabled unavailable actions, explanatory
labels, and rejection of invalid numbers before service access.
No live API requests or browser visual verification were performed in this phase.

## Phase 8 cases

| Partition | Expected result |
| --- | --- |
| `The cat sat on the mat.` | 6 words, 1 sentence, 6 syllables; ease 116.145, grade -1.45 |
| `The cat sat. A dog ran!` | 6 words, 2 sentences; ease 119.19, grade -2.62 |
| `A simple sentence.` | 3 words, 5 syllables; ease 62.79, grade approximately 5.246667 |
| Silent e, consonant-le, vowel groups, y, no vowels, accents | Counts follow the documented heuristic |
| Apostrophes, tags, mentions, hyphens, URLs, emoji, repeated punctuation | Documented words, syllables and sentence boundaries |
| Empty, whitespace, punctuation-only, number/emoji-only, URL-only | Unavailable; no displayed zero scores or division by zero |
| English, regional/case-varied English, unknown language, other languages | English scores, labeled unknown estimates, or unavailable reason |
| Per-post score map and post lookup | Exact membership; unmodifiable map; 404 for absent post |
| Overview link → details → results | Identical scores and original content, counts shown, one API load |
| Escaped author/text and empty/non-English details | No executable HTML; unavailable explanation and no score/count fields |
| Invalid page number, missing post, expired snapshot | 400, 404, and 410 respectively |
| Pending snapshot lookup | Details action returns asynchronously |

`ReadabilityServiceTest` asserts independently hand-counted examples rather than
reimplementing the algorithm in its assertions. `ReadabilityTest` checks exposed
values. Controller integration tests exercise the actual service and templates
with a mocked Mastodon boundary. Phase 8 passed 75 tests with 100% line, method,
and branch coverage across eleven handwritten production classes. Javadoc passed.
No live API or browser visual verification was performed in this phase.

## Phase 9 cases

| Partition | Expected result |
| --- | --- |
| Repeated words across posts, stop words, frequency ties | Exact aggregated counts, descending frequency, natural-order ties |
| Case, NFC-equivalent accents, accented/unaccented words, curly apostrophes | Equivalent spellings combine; accents retained; internal apostrophes preserved |
| URLs, numbers, emoji, hyphens, markers, embedded digits/underscores | Exclusions and letter-run rules match the documented policy |
| Empty page or text without words | Zero totals and empty frequencies |
| Successive analyses of different pages | No counts leak across pages |
| Caller mutation of source list or returned list | Defensive copy and immutable results |
| Hashtag link → statistics → results, repeated statistics visit | Exact ordered table, correct totals/scope, one API request |
| Empty statistics on page 3 | Clear empty state, no table, return URL preserves page number |
| Invalid page number and pending lookup failing with expiry | 400 before service access; asynchronous 410 |

`WordStatisticsServiceTest` uses synthetic content and exact ordered expected pairs.
`WordStatisticsTest` verifies both immutable model classes. Controller integration
tests exercise the real analyzer and cache with a mocked client. Phase 9 passed
84 tests with 100% line/method/branch coverage across fourteen handwritten production
classes; Javadoc also passed. No live API or browser visual verification occurred.

## Coverage rules

JaCoCo measures all compiled application classes except compiler-generated routes,
reverse routes, and Twirl templates. No handwritten production class is excluded.
The build requires 100% line and method coverage and zero missed branches.

The sbt plugin treats a class with no branches as 0% branch coverage. The extra
XML report check instead rejects any missed branch, allowing branch-free classes
without weakening the requirement once conditional logic is added. Branch coverage
for the initial home controller is therefore not applicable, rather than a measured
100% over existing branches.

HTML and XML coverage outputs are under `target/scala-2.13/jacoco/report/`.
Generated code is still exercised through route and response tests even though it
is not included in handwritten-code coverage.

## Javadoc verification

Run `sbt javadoc`. This compiles the application and tests, then documents all
handwritten Java sources together with `-private`. Standard Javadoc does not render
method-level `@author` entries. The task translates those tags in temporary source
copies into a custom author tag, leaving the original Java files unchanged.
Documentation warnings fail the task.
Inspect `target/site/javadoc/index.html` and confirm the test class and its private
`assertHomePage` helper are included.
