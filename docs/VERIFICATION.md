# Phase 10 — Coverage and full-flow verification

This records verified behavior of the working tree after Phase 9 (Word statistics) commit `689e944`.
Phase 10 (Coverage and full-flow verification) adds integration tests and documentation; it does not change production
behavior. No commit or push was performed by the assistant.

## Executed checks

Using Homebrew JDK 21:

```sh
sbt -batch test jacoco javadoc
```

- 88 JUnit tests passed with no failures or errors.
- All 14 handwritten production classes are included in JaCoCo.
- Line coverage: 286/286; branch coverage: 146/146; method coverage: 55/55.
- Only compiler-generated routing and Twirl code are excluded. Rendered templates
  are exercised by route tests, including their success, empty, and error states.
- Strict Javadoc generation passed. All 187 generated method/constructor detail
  sections, including private helpers and tests, contain rendered author entries.
- All 28 handwritten Java source classes contain author and assistance disclosures.
- `git diff --check` passed.

Reports are generated under `target/scala-2.13/jacoco/report/` and
`target/site/javadoc/`. Phase 11 (Documentation and generated reports) adds delivery copies under `docs/generated/`; see
[GENERATED_REPORTS.md](GENERATED_REPORTS.md) for regeneration and inspection.

## Full-flow integration evidence

`ApplicationFlowTest` mocks Play WS at the outbound HTTP boundary. The application
controller, client, JSON mapper, HTML-to-text converter, cache, analysis services,
routes, and templates remain real. Responses and credentials are synthetic.

| Scenario | Evidence asserted |
| --- | --- |
| First page with 40 JSON posts | 40 rendered cards; normalized text/metadata; `limit=40` on each request |
| Details and statistics from that page | Exact scores/counts; 80 words split into `cat:40`, `dog:40`; no extra HTTP call |
| Older page | Last first-page ID `61` used as `max_id`; page 2 shows its distinct content |
| Analysis of older page | Only `bird:1`, rather than aggregation with page 1 |
| Previous and repeated older navigation | Exact prior HTML; two HTTP calls total for the two pages |
| Upstream 401/403/429/503/504/500 | Appropriate 502/503/504 browser status; controlled message, no raw body or dummy token |
| Malformed JSON or 41 returned posts | 502, no partial results |
| Retry after failure | Same query reaches the API again; success is subsequently cached |
| Pending transport with equivalent searches | Actions return incomplete stages; one shared request; identical completed pages |
| Ten-minute expiry | Cached, older, details, and statistics links return 410 without HTTP calls |
| Explicit search after expiry | A new request and a new snapshot ID |

Other unit and route tests cover input boundaries, Unicode, model immutability,
concurrent loads, eviction, cancellation, timeouts, missing posts, formulas, word
ordering, and escaping. See [the testing strategy](TESTING.md) for the partitions.

## Source and repository audit

- One handwritten application controller delegates retrieval and analysis to services.
- All six controller actions return `CompletionStage<Result>`.
- Production sources contain no blocking future `join`, future `get`, or sleeps.
  The `get` calls present are asynchronous WS requests, atomic cache loading, and
  map lookup in a template; they are not blocking future waits.
- No application actors, WebSockets, or streaming API implementation are present.
- `.env` is ignored, and neither `.env` nor `conf/application.local.conf` appears
  in the available Git history. No local credential contents were opened or printed.
  This is a filename/history check, not a comprehensive secret-scanning guarantee.
- Repository docs retain the assistance record.

## Remaining delivery checks

Browser visual inspection, final clean-checkout run, dependency resolution in that
checkout, submission archive inspection, and the signed originality form remain
for Phase 12 (Final delivery checks). The diagram and report copies are prepared in Phase 11 (Documentation and generated reports). Account ownership, external team-information
submission, and signature authenticity cannot be established by these automated
tests. This verification used no live Mastodon API requests and did not restart or
stop the developer's running server.

## Phase 11 — Documentation and generated reports: report refresh

Re-ran `sbt -batch jacoco javadoc` against the Phase 10 (Coverage and full-flow verification) Java sources. All 88 tests
passed with the same full coverage counters. Copied 66 Javadoc files and 57 coverage
report files into `docs/generated/`, verified every copied file against its source,
and checked local report/documentation file links. No missing targets were found.
The Java implementation and tests were not changed in Phase 11 (Documentation and generated reports).
