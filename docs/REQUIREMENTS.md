# Delivery 1 requirements

This checklist tracks the requirements in **Project Delivery 1 v1.1**, supplied as
`SOEN 6441 D1 F26.docx`. Unchecked items are not yet verified. Implementation
choices are distinguished from assignment requirements below.

## Task allocation

| Member | Group responsibility | Primary individual task | Additional feature |
| --- | --- | --- | --- |
| Nabeel Ali | Complete group application | Readability | Word statistics |

## Group application

- [ ] Dedicated project Mastodon account and registered application, user token with `read:statuses` (section 3).
- [ ] External instance/token configuration; no credentials in source, Git history, or submission (sections 3 and 9).
- [ ] Java Play application built and run through sbt; dependencies resolved automatically (5.3).
- [ ] Single application controller, with business logic in dedicated classes (5.3).
- [ ] Hashtag input without `#`, using the direct `/api/v1/timelines/tag/{hashtag}` API without a Mastodon wrapper (4.1–4.2).
- [ ] No more than 40 posts requested at a time (4.1).
- [ ] Author, date/time, language, readable content, original link, and visibility displayed (4.2).
- [ ] API pagination for additional results (5.1).
- [ ] Cached data reused between searches and individual analyses (5.3).
- [ ] Meaningful errors and appropriate status codes for invalid input, API unavailability, HTTP/authentication errors, and malformed data (5.2).
- [ ] Empty successful searches display a clear no-results state (5.2).
- [ ] All controller actions asynchronous; no blocking future waits or sleeps in production code (5.5).
- [ ] No application actors, WebSockets, streaming API, or reactive streams (sections 1 and 7).
- [ ] Domain posts independent of REST transport, allowing future Delivery 2 adaptation (8).


## Readability and word statistics

- [ ] Show Flesch Reading Ease and Flesch–Kincaid Grade Level for post content (4.3).
- [ ] Clicking a result opens a post details page with readability information (4.3).
- [ ] Link word statistics from the search term above the results (4.3).
- [ ] Count unique words and display their frequencies in descending order (4.3).
- [ ] Use Java Streams for word-content processing (4.3).

The assignment's term “overview's description” is interpreted as post content in
the results overview. Both scores will appear there and on the details page.

## Testing and code documentation

- [ ] Write JUnit tests alongside every production class and controller action (5.6).
- [ ] Mock the API with Mockito or a factory-generated mock; no live API unit tests (5.6).
- [ ] Document equivalence partitions and assert exact results (5.6).
- [ ] Achieve 100% branch coverage of handwritten production code with JaCoCo (5.6).
- [ ] Verify `sbt run`, `sbt test`, `sbt jacoco`, `sbt javadoc`, and `sbt clean` (5.3).
- [ ] Javadoc every class, constructor, and method, including private methods and tests (5.4).
- [ ] Include `@author` on all classes and methods (5.4).
- [ ] Disclose GenAI assistance in affected class documentation and retain prompt provenance (5.4).
- [ ] Include generated HTML Javadoc covering private methods and tests (5.4).
- [ ] Add concise explanatory comments above logical code blocks (developer preference).

## Submission and demonstration

- [ ] Verify team-information submission: repository URL and individual task allocation (6).
- [ ] Include source, tests, Play configuration, templates/assets, build configuration, and README (9).
- [ ] README explains installation/run, instance/token configuration, usage, tests, architecture, assumptions, and limitations (9.2).
- [ ] Include architecture diagram showing browser, controller, service, client, REST API, cache, and analysis components (9.3).
- [ ] Include short design document covering model, responsibilities, API isolation, Streams, and Delivery 2 evolution (9.4).
- [ ] Include the genuinely signed official Expectation of Originality form (6).


## Agreed implementation defaults

- Java 21, Play Java, Twirl templates, Play WS, Jackson, jsoup, JUnit, and Mockito.
- Ordinary HTTP responses with `CompletionStage<Result>`; one application controller.
- Bounded asynchronous page cache: 100 entries, ten-minute expiry, shared pending requests, no retained failures.
- Cache keys include instance, normalized hashtag, and pagination cursor.
- Analysis applies to the displayed result page; expired analysis context requests a new search.
- Readability uses a documented English syllable heuristic. Empty or known non-English text has unavailable scores; unknown language is labeled as an English-based estimate.
- Word counts normalize case and Unicode words, preserve internal apostrophes, exclude URLs/numbers/emoji, retain stop words, and break frequency ties alphabetically.
- No database, CI deployment, or automatic browser updates in this delivery.

These defaults are project decisions, not additional requirements quoted from the assignment.
