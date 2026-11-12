# Implementation progress

Each phase includes relevant tests and documentation. The assistant stops after
each phase for review. Nabeel Ali commits and pushes manually before the next phase.

| Phase | Deliverable | Status | Suggested commit |
| --- | --- | --- | --- |
| 1 | Requirements, allocation, exclusions, setup instructions | Repository preparation complete; external setup pending | `docs: record delivery one scope and setup` |
| 2 | Play skeleton, tested home page, sbt quality tasks | Committed by Nabeel as `b584e2a` | `build: initialize Play app and quality tools` |
| 3 | Domain classes, JSON mapping, readable text | Committed by Nabeel as `9f8d7c2` | `feat: add post model and response mapping` |
| 4 | Asynchronous REST client and error handling | Committed by Nabeel as `19bcc6b` | `feat: retrieve Mastodon hashtag posts asynchronously` |
| 5 | Validation and shared bounded cache | Implemented and verified; awaiting manual commit | `feat: validate and cache hashtag searches` |
| 6 | Search controller, routes, templates, error UI | Not started | `feat: display searchable Mastodon results` |
| 7 | Cursor pagination and cached page navigation | Not started | `feat: paginate cached hashtag results` |
| 8 | Readability scores and post details page | Not started | `feat: add post readability and details` |
| 9 | Streams word analysis and statistics page | Not started | `feat: add word frequency statistics with streams` |
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
