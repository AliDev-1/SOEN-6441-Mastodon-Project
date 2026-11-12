# MAWA Mastodon Analysis Web App

SOEN 6441 Advanced Programming Practices, Delivery 1. MAWA retrieves
Mastodon hashtag posts and displays them with readability scores and word statistics.

## Current status

Search, numbered pagination, readability, post details, and word statistics are
implemented. Phase 10 (Coverage and full-flow verification) verification passed 88 tests with 100% line, method, and
branch coverage of handwritten production code. Full-flow tests mock only the
outbound HTTP boundary. Phase 11 (Documentation and generated reports) is the final
project phase and supplies the final documentation and generated reports. Outstanding
submission checks are tracked separately in the [verification record](docs/VERIFICATION.md).

## Run locally

Select JDK 21, then start Play from the repository root:

```sh
sbt run
```

Open <http://localhost:9000>. Stop the server with Ctrl+C.
See [setup instructions](docs/SETUP.md) for selecting the JDK on macOS.

## Test and document

```sh
sbt test
sbt jacoco
sbt javadoc
sbt clean
```

Tests run without Mastodon credentials or network access to Mastodon. Coverage
reports appear at `target/scala-2.13/jacoco/report/html/index.html`, and Javadoc at
`target/site/javadoc/index.html`. Javadoc includes private methods and test classes.
`clean` deletes generated outputs, so regenerate reports afterward when needed.
See [testing notes](docs/TESTING.md) for the current test cases and coverage rules.

## Usage

1. Load your local environment and start Play as described in the setup guide.
2. Open <http://localhost:9000>, enter a hashtag such as `java` without `#`, and
   click **Search posts**. Searches can be bookmarked at `/search?hashtag=java`.
3. Read the first page of results or follow **View original post**. Publication
   times are UTC. No-text posts display a clear placeholder.
4. Use the navigation above or below results: **Previous page** goes back, while
   **Next page · Older posts** advances. The current position is shown as **Page 1**,
   **Page 2**, and so on; no total is implied. Unavailable actions are disabled.
   **Back to first page** repeats the initial search.
5. **Open saved version of this page** preserves the exact displayed snapshot while it is
   retained. Expired or evicted snapshot links return 410 and ask for a new search.
6. Click an author's name or **View post details and readability** to open the saved
   post, both scores, and the word/sentence/syllable counts. **Back to results** keeps
   your page number and snapshot without another API request.
7. Select the linked hashtag above the results to open **Word statistics** for that
   saved page only. The table sorts frequencies from highest to lowest, with ties
   ordered alphabetically. **Back to results** preserves the snapshot and page number.
8. Correct invalid input or retry after an API error. Repeated normalized searches
   reuse cached results for ten minutes, subject to the 100-page retention limit.

After dependency changes (including the Caffeine addition in Phase 5 (Validation and cache)), stop the existing
server with Ctrl+C and start a fresh `sbt run`; source reload alone may retain the
old build dependencies.

## Readability assumptions

Scores estimate English text and use an approximate syllable counter. Known
non-English posts and posts without analyzable words show an unavailable message.
Unknown-language posts are labeled as English-based estimates. URLs are excluded;
words, sentence boundaries, abbreviations, and pronunciation are approximated.
Results are displayed to two decimal places without capping: negative grade levels
and Reading Ease above 100 can occur, especially for very short posts. These scores
do not measure meaning or writing quality. Details and formulas are documented in
[the design notes](docs/DESIGN.md).

## Word-count assumptions

Statistics combine all posts and languages on the displayed page, not every result
for the hashtag. Case and canonical Unicode spellings are normalized; accents remain
distinct. Internal straight and curly apostrophes are combined. URLs, numbers, and
emoji are excluded; hyphens and other separators split words. Letters in hashtags
and mentions still count. Common words such as “the” are retained. Equal frequencies
use deterministic Unicode string order, not language-specific dictionary collation.
Languages without spaces are treated as letter sequences rather than linguistically
segmented words. See the design notes for the complete policy and examples.

The application uses Java 21, Play Java, sbt, and server-rendered pages. Controller
actions and HTTP calls are asynchronous. Search and analysis reuse cached posts.

## Architecture and project layout

The single application controller delegates to a cached search service and local
analysis services. Play WS retrieves JSON asynchronously; the mapper creates
immutable posts. Twirl renders escaped HTML. Readability and word statistics read
the same saved page, without another Mastodon request. See the
[architecture diagram](docs/ARCHITECTURE.md) and [design](docs/DESIGN.md).

| Directory | Contents |
| --- | --- |
| `app/controllers`, `app/services`, `app/models` | Java application logic and domain values |
| `app/views`, `public` | Twirl templates and CSS |
| `conf`, `project`, `build.sbt` | Routes, configuration, and pinned build |
| `test` | JUnit and Mockito tests |
| `docs` | Design, verification, disclosures, and delivery instructions |
| `docs/generated` | Prepared Javadoc and coverage reports |
| `target` | Disposable build outputs, excluded from Git |

## Documentation

- [Setup instructions](docs/SETUP.md)
- [Requirements and submission checklist](docs/REQUIREMENTS.md)
- [Implementation phases](docs/PROGRESS.md)
- [Design and data mapping](docs/DESIGN.md)
- [AI assistance disclosure](docs/AI_USAGE.md)
- [Architecture diagram](docs/ARCHITECTURE.md)
- [Testing strategy](docs/TESTING.md)
- [Verification record](docs/VERIFICATION.md)
- [Generated reports](docs/GENERATED_REPORTS.md)

## Configuration

The client reads `MASTODON_INSTANCE` and `MASTODON_ACCESS_TOKEN` when it is
created. Missing settings produce a controlled error on fetch without preventing
the home page from starting. Use a dedicated project account and a user access
token with `read:statuses`. Never commit credentials. See the setup instructions
for loading `.env` before starting sbt. Restart after changing these values.
The instance must be an HTTPS origin without an API path, query, or fragment.

## Project ownership

Nabeel Ali implements the group component and readability as the primary individual
feature. Word statistics is an additional feature.
