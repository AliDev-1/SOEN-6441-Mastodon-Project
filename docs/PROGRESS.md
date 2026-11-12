# Implementation progress

Each phase includes relevant tests and documentation. The assistant stops after
each phase for review. Nabeel Ali commits and pushes manually before the next phase.

| Phase | Deliverable | Status | Suggested commit |
| --- | --- | --- | --- |
| 1 | Requirements, allocation, exclusions, setup instructions | Repository preparation complete; external setup pending | `docs: record delivery one scope and setup` |
| 2 | Play skeleton, tested home page, sbt quality tasks | Committed by Nabeel as `b584e2a` | `build: initialize Play app and quality tools` |
| 3 | Domain classes, JSON mapping, readable text | Committed by Nabeel as `9f8d7c2` | `feat: add post model and response mapping` |
| 4 | Asynchronous REST client and error handling | Committed by Nabeel as `19bcc6b` | `feat: retrieve Mastodon hashtag posts asynchronously` |
| 5 | Validation and shared bounded cache | Committed by Nabeel as `8359f87` | `feat: validate and cache hashtag searches` |
| 6 | Search controller, routes, templates, error UI | Committed by Nabeel as `19a2cb4` | `feat: display searchable Mastodon results` |
| 7 | Cursor pagination and cached page navigation | Committed by Nabeel as `a433732` | `feat: paginate cached hashtag results` |
| 8 | Readability scores and post details page | Committed by Nabeel as `fdc40ce` | `feat: add post readability and details` |
| 9 | Streams word analysis and statistics page | Implemented and verified; awaiting manual commit | `feat: add word frequency statistics with streams` |
| 10 | Coverage completion and full-flow verification | Not started | `test: complete branch coverage and failure cases` |
| 11 | Final documentation, diagram, generated Javadoc | Not started | `docs: complete design testing and API documentation` |
| 12 | Clean-checkout verification, package, demo rehearsal | Not started | `chore: prepare delivery one submission` |

## Phase 1 handoff

- Repository began with one README and one initial commit on `main`.
- GitHub remote was readable; push authentication has not been tested.
- Author identity supplied by the developer: Nabeel Ali.
- Dedicated Mastodon account, registered application, and local credentials remain pending.
- At the Phase 1 handoff, JDK 21 installation/selection remained pending; sbt was present.
- Original task allocation, rubric, and official originality form remain to be verified.
- No application exists yet, so no application tests or coverage results are claimed.

Do not mark submission or demo requirements complete merely because the code or
instructions have been prepared. Record their actual completion separately.

## Phase 2 handoff

- Installed Homebrew JDK 21 without changing the system Java default.
- Pinned Play 3.0.12 and sbt 1.13.0; added one asynchronous home controller,
  Twirl template, stylesheet, and local configuration.
- Three JUnit tests verify the direct action, home route, and linked stylesheet.
- Verified `sbt clean`, `test`, `jacoco`, and `javadoc`; `sbt run` served HTTP 200
  at `http://localhost:9000` and was stopped after the smoke check.
- Coverage for the handwritten controller: 3/3 lines and 2/2 methods; no branches
  exist yet. Generated routes and templates are excluded from the report.
- Verified generated Javadoc includes the private test helper and renders the
  author entries for classes, constructors, and methods.
- Browser visual inspection remains pending because the browser preview host was
  unavailable. The HTTP response and stylesheet were verified programmatically.
- Mastodon configuration is optional at this stage; the account and token are
  still needed before the Phase 4 live client check.
- No commit, push, or tag was created by the assistant.

## Phase 3 handoff

- Added an immutable `Post`, a stateless `HtmlText` converter, and a
  `MastodonPostMapper` that validates complete timeline responses.
- Added exact-value tests for all post fields, optional-field defaults, API order,
  unmodifiable results, malformed data, publication times, original links, and HTML.
- `sbt test jacoco javadoc` passed: 28 tests, 100% line, method, and branch coverage
  across the current handwritten production code.
- No HTTP calls or UI features were added; the mapper is ready for the Phase 4 client.
- Nabeel reported configuring the token in `.env`. Its contents were not read or
  displayed, and authentication has not been verified yet.
- No commit, push, tag, or change to the developer's running server was made.

## Phase 4 handoff

- Added an injectable singleton `MastodonClient` using Play WS and a safe
  `MastodonException` carrying the intended application status and message.
- Each request uses the configured HTTPS origin, a Bearer token, `limit=40`, an
  optional `max_id`, a ten-second timeout, and disabled redirects.
- Configuration, authentication, rate limiting, invalid data, upstream errors,
  timeouts, and connection failures have controlled messages.
- `sbt test jacoco javadoc` passed: 42 tests and 100% line, method, and branch
  coverage across the current handwritten production code.
- A separate temporary live runner called the actual client for `java` and mapped
  40 posts successfully. It loaded local configuration without displaying secrets
  or post contents. The runner was removed and is not part of the unit test suite.
- The home page is unchanged; caching and the search UI are later phases.
- No commit, push, or change to the developer's running server was made.

## Phase 5 handoff

- Added `SearchInput`, immutable `SearchPage` snapshots, and singleton `SearchService`.
- Normalized searches share pending and completed requests. Cache keys include
  instance, hashtag, and cursor; retained pages expire ten minutes after completion.
- Caffeine bounds retained pages to 100 and removes failed loads. Caller cancellation
  does not cancel a shared request. Expired or evicted snapshot links return 410.
- Tests use mocked API responses and a controllable clock; no live calls or sleeps.
- `sbt test jacoco javadoc` passed: 51 tests, 100% line, method, and branch
  coverage across all nine handwritten production classes; Javadoc passed without warnings.
- The service is ready for Phase 6; the homepage and running server are unchanged.
- No commit, push, or tag was created by the assistant.

## Phase 6 handoff

- Connected the existing singleton service through the single application controller.
- Added a GET search form, first-page results with all required metadata, shared
  layout, responsive styles, empty-results state, and safe error responses.
- Templates escape input and post content; original URLs come from validated models.
- Route tests mock external work and verify input validation, cached reuse, all
  displayed fields, escaping, empty results, asynchronous completion, and errors.
- The existing localhost process reported a missing Caffeine dependency from Phase 5;
  restart that server to reload build dependencies. It was not stopped or modified.
- Browser preview was unavailable; rendered HTML is verified by automated route tests.
- `sbt test jacoco javadoc` passed: 57 tests, 100% line/method/branch coverage
  across handwritten production code, and successful Javadoc generation.
- Pagination controls and analyses remain later phases. No commit or push was made.

## Phase 7 handoff

- Added older-page requests derived from the last post's opaque ID, with the existing
  40-post limit and cache key separating each cursor.
- Added cached snapshot routes, stable page links, predecessor links, and a first-page link.
- Short pages still allow older requests; empty or nonadvancing pages stop navigation.
  Empty older results have a distinct end-of-results message.
- Cached predecessor navigation never calls the API. Expired source snapshots fail
  with 410 before any older request; failed API loads retain their safe error response.
- `sbt test jacoco javadoc` passed: 64 tests, 100% line/method/branch coverage across
  handwritten production code, and successful Javadoc generation.
- Route tests follow rendered links using the real cache and a mocked API; no live
  API calls were made. Browser visual verification remains pending.
- No commit, push, tag, or server restart was performed.

### Phase 7 navigation refinement

- Kept all four actions with clearer labels and separate browsing/utility rows.
- Repeated navigation above and below results, showing the current page number.
- Page numbers travel with navigation links without fragmenting the shared cache.
- Added saved-link explanation and visible disabled states for unavailable actions.
- Expanded route checks for numbered links, invalid numbers, and layout content.

## Phase 8 handoff

- Added immutable readability measurements and a stateless local calculator with
  Flesch Reading Ease and Flesch–Kincaid Grade Level formulas.
- Added overview estimates and cached post details with metadata, counts, formula
  explanations, and a return link preserving the result page number.
- Added explicit handling for empty/non-English text and unknown-language estimates.
- Documented word, sentence, and syllable heuristics, rounding, and limitations.
- Tests cover exact hand-counted formulas, text/language partitions, details links,
  matching overview/detail values, escaping, no-refetch behavior, and 400/404/410 errors.
- `sbt test jacoco javadoc` passed: 75 tests, 100% line/method/branch coverage across
  all eleven handwritten production classes, and successful Javadoc generation.
- No live Mastodon request, commit, push, tag, or server restart was performed.
- Browser visual verification remains pending; rendered responses are route-tested.

## Phase 9 handoff

- Added immutable frequency/statistics models and a stateless Java Streams analyzer.
- Linked the hashtag above results to statistics for that exact retained page.
- Added total/unique counts, descending frequencies with deterministic ties, an empty
  state, counting explanations, and a numbered return link.
- Documented Unicode, apostrophe, URL, number, separator, and multilingual behavior.
- Tests verify exact frequencies/order, normalization, page isolation, immutable
  results, no-refetch route navigation, empty results, and asynchronous errors.
- `sbt test jacoco javadoc` passed: 84 tests, 100% line/method/branch coverage across
  fourteen handwritten production classes, and successful Javadoc generation.
- No live API call, commit, push, tag, or server restart was performed. Browser visual
  verification remains pending; route tests verify rendered HTML.
